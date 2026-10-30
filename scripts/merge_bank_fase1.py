#!/usr/bin/env python3
"""Fase 1: fusion del banco sin duplicados ni colisiones (solo lectura + tmp/).

Entradas (relativas a la raiz del repo pruebaUdea):
  - rutaUdea/app/src/main/assets/questions/questions.jsonl   (BASE, 200, autoritativa)
  - questions_imagenes_corregidas.jsonl                      (CURADA, 212 = base + 12)
  - udea_dataset/dataset/preguntas_con_trazabilidad.jsonl    (TRAZ, 400, IDs planos)

Salidas (rutaUdea/tmp/fase1/, NO toca assets ni Firestore):
  - banco_fusionado.jsonl, cuarentena_garbage.jsonl, reporte_fase1.json

Reglas:
  1. La BASE se conserva integra (ninguna fila se modifica ni se renombra).
  2. Todo ID nuevo se normaliza a `{ns}_{id-origen}` (rec2_1, rec3_RL-0001...).
     Los IDs planos RL-0001 x4 colisionarian entre si; con namespace no.
  3. Dedup exacto por ID nuevo y semantico por fingerprint
     sha256(pregunta normalizada). Coincidencia semantica con la base
     o entre nuevas -> cuarentena + reporte (no se inserta).
  4. Las filas de TRAZ con tipo_gestion=garbage van a cuarentena, no al banco.
  5. Todo lo nuevo de TRAZ entra con estado=pendiente (+flag _aprobable
     si cumple 4 opciones + respuesta A-D + no requiere imagen).

Uso:
  python3 scripts/merge_bank_fase1.py [--dry-run]
"""
import hashlib
import json
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path

RUTA_UDEA = Path(__file__).resolve().parent.parent
REPO = RUTA_UDEA.parent
BASE = RUTA_UDEA / "app/src/main/assets/questions/questions.jsonl"
CURADA = REPO / "questions_imagenes_corregidas.jsonl"
TRAZ = REPO / "udea_dataset/dataset/preguntas_con_trazabilidad.jsonl"
OUT_DIR = RUTA_UDEA / "tmp" / "fase1"

NS = {
    "recopilacion-2": "rec2",
    "recopilacion-3": "rec3",
    "recopilacion-4": "rec4",
    "recopilacion-6": "rec6",
    "recopilacion-7": "rec7",
}
SECCION_AREA = {"RL": "razonamiento_logico", "CL": "competencia_lectora"}
LETRAS = ["A", "B", "C", "D"]


def ns_de(fuente):
    fuente = (fuente or "").strip()
    if fuente in NS:
        return NS[fuente]
    slug = re.sub(r"[^a-z0-9]+", "", fuente.lower())[:12] or "src"
    return slug


def nid(fuente, id_origen):
    """ID namespaced determinista: misma (fuente,id) -> mismo nid siempre."""
    ns = ns_de(fuente)
    oid = re.sub(r"\s+", "", str(id_origen or "").strip())
    if oid.lower().startswith(ns.lower() + "_"):
        return oid
    return f"{ns}_{oid}"


def norm(texto):
    t = unicodedata.normalize("NFKD", str(texto or ""))
    t = "".join(c for c in t if not unicodedata.combining(c))
    t = t.lower()
    t = re.sub(r"[^a-z0-9 ]", " ", t)
    return re.sub(r"\s+", " ", t).strip()


def fingerprint(pregunta, opciones=None):
    base = norm(pregunta)
    if opciones:
        if isinstance(opciones, dict):
            vals = [opciones.get(k, "") for k in LETRAS]
        else:
            vals = list(opciones)
        base += "|" + "|".join(norm(v) for v in vals)
    return hashlib.sha256(base.encode("utf-8")).hexdigest()[:16]


def load_jsonl(path):
    rows = []
    with open(path, encoding="utf-8") as f:
        for i, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError as e:
                print(f"AVISO {path} linea {i}: JSON invalido ({e}), se omite")
    return rows


def traz_a_maestro(r, nuevo_id):
    """Convierte una fila de trazabilidad al schema maestro como pendiente."""
    area = SECCION_AREA.get(str(r.get("seccion", "")).strip().upper())
    opc = r.get("opciones") or []
    if isinstance(opc, dict):
        opc_dict = {k: str(opc.get(k, "") or "").strip() for k in LETRAS}
    else:
        opc_list = [str(v or "").strip() for v in opc]
        opc_dict = (
            dict(zip(LETRAS, opc_list))
            if len(opc_list) == 4 and all(opc_list)
            else {}
        )
    resp = str(r.get("respuesta_correcta", "") or "").strip().upper()
    resp = resp if resp in LETRAS else None
    req_img = str(r.get("requiere_imagen", "") or "").strip().upper() == "SI"
    resol = str(r.get("resoluble_texto", "") or "").strip().upper()
    desc = str(r.get("descripcion_contexto", "") or "").strip()
    if desc.lower() in ("", "ninguno", "none", "-"):
        desc = ""
    aprobable = bool(area) and len(opc_dict) == 4 and resp and not req_img
    return {
        "id": nuevo_id,
        "area": area or "",
        "subtema": "",
        "competencia": None,
        "tipo_texto": None,
        "dificultad": "media",
        "texto_base": None,
        "contexto": desc,
        "pregunta": r.get("pregunta") or "",
        "opciones": opc_dict,
        "respuesta_correcta": resp,
        "explicacion": None,
        "fuente": r.get("fuente") or "",
        "tipo_fuente": "recopilacion",
        "anio": None,
        "pagina": r.get("pagina_pdf") or None,
        "licencia": "",
        "es_original": False,
        "verificada": False,
        "duplicado": False,
        "estado": "pendiente",
        # Campos Fase 1-3 (la app los ignorara hasta la migracion Fase 2):
        "figura_id": None,
        "imagen_url": None,
        "requiere_imagen": req_img,
        "resoluble_texto": resol,
        "_origen": f"trazabilidad:{r.get('fuente')}:{r.get('id')}",
        "_tipo_gestion": r.get("tipo_gestion") or "",
        "_aprobable": aprobable,
        "_opciones_raw": None if opc_dict else opc,
        "_area_pendiente": area is None,
    }


def main():
    dry_run = "--dry-run" in sys.argv
    for p in (BASE, CURADA, TRAZ):
        if not p.exists():
            print(f"ERROR: no existe {p}")
            sys.exit(1)

    base = load_jsonl(BASE)
    curada = load_jsonl(CURADA)
    traz = load_jsonl(TRAZ)

    vistos_id = set()
    vistos_fp = {}
    dup_id_base = []
    for r in base:
        _id = str(r.get("id", ""))
        if _id in vistos_id:
            dup_id_base.append(_id)
        vistos_id.add(_id)
        vistos_fp.setdefault(fingerprint(r.get("pregunta"), r.get("opciones")), _id)

    fusion = list(base)
    cuarentena = []
    rep = Counter()
    detalle = {
        "colisiones_evitadas": [],
        "omitidas_por_id_existente": [],
        "duplicados_semanticos": [],
        "renombres_curada": {},
        "garbage": 0,
    }
    rep["base"] = len(base)

    # --- Capa curada: solo aporta lo que la base no tiene (las 12) ---
    for r in curada:
        _id = str(r.get("id", ""))
        if _id in vistos_id:
            rep["curada_ya_en_base"] += 1
            continue
        nuevo = nid(r.get("fuente"), _id)
        if nuevo in vistos_id:
            detalle["omitidas_por_id_existente"].append(nuevo)
            rep["curada_colision_nid"] += 1
            continue
        fp = fingerprint(r.get("pregunta"), r.get("opciones"))
        if fp in vistos_fp:
            detalle["duplicados_semanticos"].append(
                {"nuevo": nuevo, "igual_a": vistos_fp[fp], "capa": "curada"}
            )
            rep["curada_dup_semantico"] += 1
            continue
        r2 = dict(r)
        r2["id"] = nuevo
        r2["figura_id"] = None
        r2["imagen_url"] = None
        r2["requiere_imagen"] = False
        r2["_origen"] = f"curada:{r.get('fuente')}:{_id}"
        detalle["renombres_curada"][_id] = nuevo
        detalle["colisiones_evitadas"].append(f"curada:{_id} -> {nuevo}")
        fusion.append(r2)
        vistos_id.add(nuevo)
        vistos_fp.setdefault(fp, nuevo)
        rep["curada_agregadas"] += 1

    # --- Capa trazabilidad: garbage a cuarentena, resto a pendiente ---
    for r in traz:
        fuente, oid = r.get("fuente"), str(r.get("id", ""))
        nuevo = nid(fuente, oid)
        if nuevo in vistos_id:
            rep["traz_id_existente"] += 1
            detalle["omitidas_por_id_existente"].append(f"{fuente}:{oid} -> {nuevo}")
            continue
        gestion = (r.get("tipo_gestion") or "").strip()
        if gestion == "garbage":
            cuarentena.append({"motivo": "garbage", "id_propuesto": nuevo, "fila": r})
            rep["traz_garbage_cuarentena"] += 1
            vistos_id.add(nuevo)
            continue
        m = traz_a_maestro(r, nuevo)
        fp = fingerprint(m["pregunta"], m["opciones"] or m["_opciones_raw"])
        if fp in vistos_fp:
            detalle["duplicados_semanticos"].append(
                {"nuevo": nuevo, "igual_a": vistos_fp[fp], "capa": "trazabilidad"}
            )
            cuarentena.append(
                {"motivo": "duplicado_semantico_de:" + str(vistos_fp[fp]),
                 "id_propuesto": nuevo, "fila": r}
            )
            rep["traz_dup_semantico"] += 1
            vistos_id.add(nuevo)
            continue
        fusion.append(m)
        vistos_id.add(nuevo)
        vistos_fp.setdefault(fp, nuevo)
        rep["traz_agregadas_pendiente"] += 1
        if m["_aprobable"]:
            rep["traz_aprobables_para_revisar"] += 1

    estados = Counter(str(r.get("estado")) for r in fusion)
    reporte = {
        "entradas": {"base": len(base), "curada": len(curada), "trazabilidad": len(traz)},
        "fusionado": len(fusion),
        "cuarentena": len(cuarentena),
        "estados_fusionado": dict(estados),
        "detalle_por_capa": dict(rep),
        "ids_base_duplicados": dup_id_base,
        "ejemplos_colision_evitada": [
            "RL-0001 plano x4 -> rec3_RL-0001, rec4_RL-0001, rec6_RL-0001, rec7_RL-0001",
            "id '1' plano (rec2) -> rec2_1 (ya no choca con futuros rec3_1...)",
        ],
    }

    print(f"BASE={len(base)} CURADA={len(curada)} TRAZ={len(traz)}")
    print(f"FUSIONADO={len(fusion)} estados={dict(estados)}")
    print(f"CUARENTENA={len(cuarentena)} | {dict(rep)}")
    print(f"Renombres curada (12 esperados): {len(detalle['renombres_curada'])}")
    print(f"Dups semanticos nuevos: {len(detalle['duplicados_semanticos'])}")

    if dry_run:
        print("DRY-RUN: no se escribio nada.");
        return

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with open(OUT_DIR / "banco_fusionado.jsonl", "w", encoding="utf-8") as f:
        for r in fusion:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    with open(OUT_DIR / "cuarentena_garbage.jsonl", "w", encoding="utf-8") as f:
        for r in cuarentena:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    with open(OUT_DIR / "reporte_fase1.json", "w", encoding="utf-8") as f:
        json.dump({"reporte": reporte, "detalle": detalle}, f, ensure_ascii=False, indent=2)
    print(f"Escrito en {OUT_DIR}/")


if __name__ == "__main__":
    main()

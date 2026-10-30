#!/usr/bin/env python3
"""Fase 5: construye el asset `questions.jsonl` desde el banco fusionado.

Reglas de estado (5.2, identicas a las del upload script):
  - `deshabilitado` se preserva siempre (curado manual).
  - `aprobado` se conserva SOLO si la fila es aprobable (enunciado + 4
    opciones + respuesta A-D), no requiere imagen y su respuesta es
    CONFIABLE (origen base/curada/restaurada, `_curaduria`, o verificada).
    Las 26 filas base marcadas `aprobado` sin respuesta caen a `pendiente`
    (en la app serian preguntas sin respuesta correcta: siempre "fallan").
  - Las filas de trazabilidad NUNCA se auto-promueven: aunque traigan letra,
    124 tienen `clave_disponible=NO` (procedencia sin verificar). Quedan
    `pendiente` para validacion en el panel admin. Idem el upload script.
  - `aprobado_con_imagen` solo con `imagen_url` presente (nadie aun).

El asset conserva el schema maestro que lee QuestionJsonlLoader
(+ `imagen_url` de Fase 2) y descarta campos internos `_ *` y de pipeline
(figura_id/imagen_path se quedan en el fusionado para el upload).

Uso:
  python3 scripts/build_asset_fase5.py [--write]   # sin --write: dry-run
"""
import json
import shutil
import sys
from collections import Counter
from pathlib import Path

RUTA_UDEA = Path(__file__).resolve().parent.parent
BANCO = RUTA_UDEA / "tmp/fase1/banco_fusionado.jsonl"
ASSET = RUTA_UDEA / "app/src/main/assets/questions/questions.jsonl"
DIFF = RUTA_UDEA / "tmp/fase1/DIFF_FASE5.md"
LETRAS = ["A", "B", "C", "D"]

ASSET_KEYS = ["id", "area", "subtema", "competencia", "tipo_texto", "dificultad",
              "texto_base", "contexto", "pregunta", "opciones", "respuesta_correcta",
              "explicacion", "fuente", "tipo_fuente", "anio", "pagina", "licencia",
              "es_original", "verificada", "duplicado", "estado", "imagen_url"]


def normalizar_opciones(opc):
    if isinstance(opc, dict):
        return {l: str(opc.get(l) or "").strip() for l in LETRAS}
    vals = [str(v or "").strip() for v in (opc or [])]
    vals += [""] * (4 - len(vals))
    return dict(zip(LETRAS, vals[:4]))


def aprobable(r):
    o = normalizar_opciones(r.get("opciones"))
    resp = str(r.get("respuesta_correcta") or "").strip().upper()
    return (all(o[l] for l in LETRAS) and resp in LETRAS
            and bool(str(r.get("pregunta") or "").strip()))


def requiere_imagen(r):
    return r.get("requiere_imagen") is True or str(
        r.get("requiere_imagen") or "").strip().upper() == "SI"


def confiable(r):
    if r.get("verificada") is True or r.get("_curaduria"):
        return True
    origen = r.get("_origen") or ""
    return (not origen or origen.startswith("curada")
            or origen.startswith("restaurada"))


def estado_final(r):
    if r.get("estado") == "deshabilitado":
        return "deshabilitado"
    if not aprobable(r):
        return "pendiente"
    if requiere_imagen(r):
        url = str(r.get("imagen_url") or "").strip()
        if url and confiable(r):
            return "aprobado_con_imagen"
        return "pendiente"
    return "aprobado" if confiable(r) else "pendiente"


def main():
    write = "--write" in sys.argv
    fus = [json.loads(l) for l in BANCO.read_text(encoding="utf-8").splitlines()
           if l.strip()]
    asset = []
    cambios = []
    for r in fus:
        nuevo = estado_final(r)
        if nuevo != r.get("estado"):
            cambios.append((r["id"], r.get("estado"), nuevo))
        fila = {k: r.get(k) for k in ASSET_KEYS}
        fila["opciones"] = normalizar_opciones(r.get("opciones"))
        fila["estado"] = nuevo
        url = str(r.get("imagen_url") or "").strip()
        fila["imagen_url"] = url or None
        asset.append(fila)

    ids = [r["id"] for r in asset]
    assert len(ids) == len(set(ids)), "IDs duplicados en asset"
    assert all(r["id"] and r["area"] for r in asset), "faltan id/area"
    por_estado = {r["id"]: r["estado"] for r in asset}
    sin_pregunta = [r["id"] for r in asset if not r["pregunta"]]
    assert all(por_estado[i] == "pendiente" for i in sin_pregunta), \
        f"fila sin pregunta y no pendiente: {sin_pregunta}"
    print(f"filas sin pregunta (todas pendiente): {len(sin_pregunta)}")
    est = Counter(r["estado"] for r in asset)
    rl = sum(1 for r in asset if r["estado"] == "aprobado"
             and r["area"] == "razonamiento_logico")
    cl = sum(1 for r in asset if r["estado"] == "aprobado"
             and r["area"] == "competencia_lectora")
    print(f"asset={len(asset)} estados={dict(est)} RL_aprob={rl} CL_aprob={cl}")
    print(f"cambios de estado vs fusionado: {len(cambios)}")
    for i, viejo, nuevo in cambios[:15]:
        print(f"  {i}: {viejo} -> {nuevo}")
    assert rl >= 40 and cl >= 40, "simulacro sin cobertura (40/area)"

    diff_md = ["# DIFF Fase 5: asset anterior (200) vs nuevo",
               f"\n- Filas: {len(asset)}; estados: {dict(est)}",
               f"- RL aprobadas: {rl}; CL aprobadas: {cl}",
               f"\n## Cambios de estado aplicados ({len(cambios)})",
               "| id | antes | despues |",
               "|---|---|---|"]
    diff_md += [f"| {i} | {v} | {n} |" for i, v, n in cambios]
    diff_md += ["\n## Criterio (plan 5.2)",
                "- `deshabilitado` preservado; `aprobado` solo con respuesta "
                "confiable + 4 opciones + sin imagen pendiente.",
                "- 124 filas de trazabilidad con letra pero `clave_disponible=NO` "
                "quedan `pendiente` (lote de validacion para el panel admin)."]

    if not write:
        print("DRY-RUN: asset no modificado. Usa --write.")
        return
    shutil.copy(ASSET, str(ASSET) + ".bak_fase4")
    with open(ASSET, "w", encoding="utf-8") as f:
        for r in asset:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    DIFF.write_text("\n".join(diff_md) + "\n", encoding="utf-8")
    print(f"Asset escrito ({ASSET}), diff en {DIFF}.")


if __name__ == "__main__":
    main()

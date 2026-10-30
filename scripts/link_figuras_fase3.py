#!/usr/bin/env python3
"""Fase 3: vincula figuras verificadas a preguntas del banco fusionado.

Lee udea_dataset/figuras/manifest.json (figuras recortadas y verificadas
visualmente) y asigna `figura_id` + `imagen_path` a las filas de
tmp/fase1/banco_fusionado.jsonl cuya (fuente, numero) coincide.

El vínculo es por NUMERO DE PREGUNTA EN EL PDF (verdad visual del render),
no por los IDs RL-XXXX del HTML (no confiables). No toca `estado`:
el upload script promueve a `aprobado_con_imagen` solo cuando la URL existe.

Uso: python3 scripts/link_figuras_fase3.py [--dry-run]
"""
import json
import shutil
import sys
from collections import Counter
from pathlib import Path

RUTA_UDEA = Path(__file__).resolve().parent.parent
REPO = RUTA_UDEA.parent
MANIFEST = REPO / "udea_dataset/figuras/manifest.json"
TRAZ = REPO / "udea_dataset/dataset/preguntas_con_trazabilidad.jsonl"
BANCO = RUTA_UDEA / "tmp/fase1/banco_fusionado.jsonl"

NS = {"recopilacion-2": "rec2", "recopilacion-3": "rec3",
      "recopilacion-4": "rec4", "recopilacion-6": "rec6",
      "recopilacion-7": "rec7"}


def nid(fuente, oid):
    ns = NS.get((fuente or "").strip())
    o = "".join(str(oid or "").split())
    if ns and not o.lower().startswith(ns.lower() + "_"):
        return f"{ns}_{o}"
    return o


def main():
    dry = "--dry-run" in sys.argv
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    num_a_traz = {}
    for line in TRAZ.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        r = json.loads(line)
        num_a_traz.setdefault((r.get("fuente"), str(r.get("numero"))), []).append(
            str(r.get("id")))
    filas = [json.loads(l) for l in
             BANCO.read_text(encoding="utf-8").splitlines() if l.strip()]
    por_id = {r["id"]: r for r in filas}

    rep = Counter()
    detalle = []
    for m in manifest:
        for num in m["numeros"]:
            traz_ids = num_a_traz.get((m["fuente"], str(num)), [])
            if not traz_ids:
                rep["sin_fila_traz"] += 1
                detalle.append(f"{m['fig']}: sin fila traz ({m['fuente']},{num})")
                continue
            for tid in traz_ids:
                fid = nid(m["fuente"], tid)
                row = por_id.get(fid)
                if row is None:
                    rep["fuera_del_fusionado"] += 1
                    detalle.append(f"{m['fig']}: {fid} fuera del fusionado "
                                   f"(cuarentena/garbage)")
                    continue
                if row.get("figura_id") and row["figura_id"] != m["fig"]:
                    rep["conflicto"] += 1
                    detalle.append(f"{m['fig']}: {fid} ya tiene "
                                   f"{row['figura_id']}, se conserva")
                    continue
                row["figura_id"] = m["fig"]
                row["imagen_path"] = m["archivo"]
                if not row.get("requiere_imagen"):
                    row["requiere_imagen"] = True
                rep["vinculadas"] += 1

    print(f"figuras={len(manifest)} filas_vinculadas={rep['vinculadas']} "
          f"sin_traz={rep['sin_fila_traz']} fuera={rep['fuera_del_fusionado']} "
          f"conflictos={rep['conflicto']}")
    for d in detalle:
        print("  " + d)
    with_fig = sum(1 for r in filas if r.get("figura_id"))
    print(f"filas con figura en banco: {with_fig}/{len(filas)}")
    if dry:
        print("DRY-RUN: no se escribio nada.")
        return
    shutil.copy(BANCO, str(BANCO) + ".bak_fase2")
    with open(BANCO, "w", encoding="utf-8") as f:
        for r in filas:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    print(f"Banco actualizado (backup .bak_fase2).")


if __name__ == "__main__":
    main()

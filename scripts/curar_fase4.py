#!/usr/bin/env python3
"""Fase 4: curaduria de preguntas estructurables con figura verificada.

Promueve a `aprobado` SOLO filas que cumplen todo:
  - texto de pregunta y opciones A-D transcritos del PDF (renders vistos),
  - respuesta matematicamente univoca o logicamente necesaria,
  - autocontenidas (requiere_imagen=False) o con figura ya vinculada.

El resto queda `pendiente` con su figura (sin inventar respuestas).
Las 9 promociones estan hardcodeadas abajo con su derivacion; cada una fue
verificada contra el render del PDF en tmp/../udea_dataset/figuras/pages/.

Uso: python3 scripts/curar_fase4.py [--dry-run]
"""
import json
import shutil
import sys
from collections import Counter
from pathlib import Path

RUTA_UDEA = Path(__file__).resolve().parent.parent
BANCO = RUTA_UDEA / "tmp/fase1/banco_fusionado.jsonl"

# (fuente, numero-pdf) -> curaduria verificada.
CURADAS = {
    ("recopilacion-3", 29): {
        "pregunta": "De las afirmaciones siguientes, la única verdadera es:",
        "opciones": {
            "A": "El 80% de la población encuestada son mujeres.",
            "B": "El 50% de los hombres utilizan como medio principal, la televisión.",
            "C": ("El porcentaje de los hombres y el porcentaje de las mujeres, referidos "
                  "a su respectivo sexo, que utilizan como medio principal la radio, es igual."),
            "D": "El 20% de las mujeres, utiliza como medio principal, el periódico.",
        },
        "respuesta_correcta": "B",
        "explicacion": "Hombres que usan TV: 120/240 = 50%. (A: 40%; C: 33.3% vs 50%; D: 25%.)",
        "contexto": ("Encuesta n=400. Internet 10/20/30; Periódico 30/40/70; Radio 80/80/160; "
                     "Televisión 120/20/140; TOTAL 240/160/400 (Hombres/Mujeres/Total)."),
        "requiere_imagen": False,
    },
    ("recopilacion-3", 30): {
        "pregunta": "De las afirmaciones siguientes, la única falsa es:",
        "opciones": {
            "A": ("El porcentaje relativo a los hombres que eligen la televisión, es igual "
                  "al porcentaje relativo a las mujeres que eligen la radio."),
            "B": ("Con relación al total de personas que eligieron la Internet, "
                  "el 33.3% son hombres."),
            "C": ("El 25% de la población total hizo su elección entre la Internet "
                  "y el periódico."),
            "D": ("Con relación al total de personas que eligieron la radio, "
                  "el 40% son mujeres."),
        },
        "respuesta_correcta": "D",
        "explicacion": ("A: 50% = 50% ✓; B: 10/30 = 33.3% ✓; C: 100/400 = 25% ✓; "
                        "D: mujeres en radio = 80/160 = 50%, no 40% ✗."),
        "contexto": ("Encuesta n=400. Internet 10/20/30; Periódico 30/40/70; Radio 80/80/160; "
                     "Televisión 120/20/140; TOTAL 240/160/400 (Hombres/Mujeres/Total)."),
        "requiere_imagen": False,
    },
    ("recopilacion-3", 32): {
        "pregunta": ("Las coordenadas en X y en Y del segmento de orden 10 "
                     "son respectivamente:"),
        "opciones": {"A": "-1 y 5", "B": "-1.5 y 5.5", "C": "-0.5 y 5.5", "D": "0 y 6"},
        "respuesta_correcta": "C",
        "explicacion": "X = -5+(n-1)*0.5; Y = 1+(n-1)*0.5; orden 10 → (-0.5, 5.5).",
        "contexto": "Órdenes 1ª-5ª: X = -5, -4.5, -4, -3.5, -3; Y = 1, 1.5, 2, 2.5, 3.",
        "requiere_imagen": False,
    },
}

# NOTA: rec7 Q3 (puerta B) se restaura via RESTAURAR (su fila esta en cuarentena).

# Filas en cuarentena (texto garbullado) que se RESTAURAN como filas limpias
# con transcripcion verificada del PDF. IDs deterministas Fase 1 (libres,
# pues la fila original no entro al fusionado). Figura vinculada incluida.
RESTAURAR = {
    ("recopilacion-3", "RL-0002", 2, "FIG-041"): {
        "pregunta": "El número de partidos que se jugaron en la eliminatoria fue:",
        "opciones": {"A": "3", "B": "4", "C": "5", "D": "6"},
        "respuesta_correcta": "D",
        "explicacion": "Todos contra todos con 4 equipos: C(4,2) = 6.",
        "contexto": ("Tabla PJ/PG/PP/PE — A: -/1/-/2; B: -/Z/2/-; C: 3/2/-/X; "
                     "E: 3/Y/-/0. C le ganó a B y E perdió con A."),
    },
    ("recopilacion-4", "RL-0001", 1, "FIG-025"): {
        "pregunta": ("En la figura las cuatro circunferencias son tangentes y las "
                     "circunferencias de centros en A, B y C tienen radio igual a "
                     "2 unidades. Entonces el perímetro del triángulo ABC es:"),
        "opciones": {"A": "8", "B": "12", "C": "14", "D": "16"},
        "respuesta_correcta": "B",
        "explicacion": "Cada lado une dos centros: 2+2 = 4. Perímetro = 3x4 = 12.",
        "contexto": "",
    },
    ("recopilacion-4", "RL-0002", 2, "FIG-045"): {
        "pregunta": ("Una barra de acero en forma de paralelepípedo rectangular, con "
                     "dimensiones 2 cm x 3 cm x 4 cm, se funde para formar tres cubos "
                     "de igual volumen. La longitud del lado de cada cubo en cm es:"),
        "opciones": {"A": "1", "B": "2", "C": "3", "D": "4"},
        "respuesta_correcta": "B",
        "explicacion": "Volumen 24/3 = 8 por cubo; lado = raíz cúbica de 8 = 2.",
        "contexto": "",
    },
    ("recopilacion-4", "RL-0003", 3, "FIG-026"): {
        "pregunta": ("La siguiente figura consta de nueve cubos pegados. Usando esta "
                     "figura como base, la menor cantidad de cubitos que faltan para "
                     "construir un cubo sólido es:"),
        "opciones": {"A": "18", "B": "27", "C": "55", "D": "64"},
        "respuesta_correcta": "A",
        "explicacion": "La figura cabe en un cubo de 3x3x3 = 27; faltan 27-9 = 18.",
        "contexto": "",
    },
    ("recopilacion-4", "RL-0008", 8, "FIG-047"): {
        "pregunta": ("Se tiene una caja de caras rectangulares cuyo volumen es igual a "
                     "1000 cm³. Si el largo es cuatro veces el ancho y la altura es el "
                     "doble del ancho, entonces el área superficial de la caja en cm² es:"),
        "opciones": {"A": "1000", "B": "800", "C": "700", "D": "500"},
        "respuesta_correcta": "C",
        "explicacion": "8a³ = 1000 → a = 5; superficie = 2(100+200+50) = 700.",
        "contexto": "",
    },
    ("recopilacion-7", "RL-0003", 3, "FIG-050"): {
        "pregunta": ("Aceptando como verdadera la siguiente afirmación: Si un paciente se "
                     "encuentra en el interior del quirófano, entonces pasó necesariamente "
                     "por la puerta B. La única afirmación válida lógicamente es:"),
        "opciones": {
            "A": "Si un paciente ingresó por B, necesariamente se encuentra en el quirófano.",
            "B": ("Si un paciente no se encuentra en el quirófano, necesariamente "
                  "no ingresó por la puerta B."),
            "C": ("Si un paciente ingresa por la puerta A y no se encuentra en el quirófano, "
                  "necesariamente ha pasado por la puerta B."),
            "D": ("Si un paciente no pasó por la puerta B, necesariamente "
                  "no se encuentra en el quirófano."),
        },
        "respuesta_correcta": "D",
        "explicacion": "D es la contrarrecíproca (p→q ≡ ¬q→¬p); las demás son recíproca/inversa.",
        "contexto": ("Consultorio con puertas A (sala de espera), B (paso a quirófano) y "
                     "C; cerraduras A:3, B:3, C:2."),
    },
}


def main():
    dry = "--dry-run" in sys.argv
    traz_num = {}
    import json as J
    for line in (RUTA_UDEA.parent / "udea_dataset/dataset/preguntas_con_trazabilidad.jsonl").read_text(
            encoding="utf-8").splitlines():
        if line.strip():
            r = J.loads(line)
            traz_num[(r.get("fuente"), str(r.get("numero")))] = str(r.get("id"))

    NS = {"recopilacion-2": "rec2", "recopilacion-3": "rec3", "recopilacion-4": "rec4",
          "recopilacion-6": "rec6", "recopilacion-7": "rec7"}

    def nid(fuente, oid):
        ns = NS.get(fuente)
        o = "".join(str(oid).split())
        if ns and not o.lower().startswith(ns.lower() + "_"):
            return f"{ns}_{o}"
        return o

    filas = [json.loads(l) for l in BANCO.read_text(encoding="utf-8").splitlines() if l.strip()]
    por_id = {r["id"]: r for r in filas}
    rep = Counter()
    for (fuente, num), cur in CURADAS.items():
        tid = traz_num.get((fuente, str(num)))
        if not tid:
            print(f"AVISO: sin fila traz ({fuente},{num})");
            rep["sin_traz"] += 1
            continue
        fid = nid(fuente, tid)
        row = por_id.get(fid)
        if row is None:
            print(f"AVISO: {fid} fuera del fusionado (cuarentena)");
            rep["fuera"] += 1
            continue
        # Validacion: 4 opciones no vacias y respuesta en A-D.
        ops = cur["opciones"]
        assert set(ops) == {"A", "B", "C", "D"} and all(str(v).strip() for v in ops.values())
        assert cur["respuesta_correcta"] in "ABCD"
        row["pregunta"] = cur["pregunta"]
        row["opciones"] = dict(ops)
        row["respuesta_correcta"] = cur["respuesta_correcta"]
        row["explicacion"] = cur["explicacion"]
        if "contexto" in cur:
            row["contexto"] = cur["contexto"]
        row["requiere_imagen"] = cur["requiere_imagen"]
        row["estado"] = "aprobado"
        row["_curaduria"] = "fase4_transcripcion_render_verificado"
        rep["promovidas"] += 1
        print(f"  {fid} -> aprobado ({cur['respuesta_correcta']})")

    # Restaurar filas en cuarentena con transcripcion verificada.
    manifest = {m["fig"]: m for m in json.loads(
        (RUTA_UDEA.parent / "udea_dataset/figuras/manifest.json").read_text(encoding="utf-8"))}
    for (fuente, tid, num, fig), cur in RESTAURAR.items():
        fid = nid(fuente, tid)
        if fid in por_id:
            print(f"AVISO: {fid} ya existe en fusionado, no se restaura");
            rep["restaurar_colision"] += 1
            continue
        m = manifest[fig]
        assert num in m["numeros"], f"{fig} no cubre Q{num}"
        ops = cur["opciones"]
        assert set(ops) == {"A", "B", "C", "D"} and all(str(v).strip() for v in ops.values())
        assert cur["respuesta_correcta"] in "ABCD"
        row = {
            "id": fid,
            "area": "razonamiento_logico",
            "subtema": "general",
            "competencia": None,
            "tipo_texto": None,
            "dificultad": "media",
            "texto_base": None,
            "contexto": cur.get("contexto", ""),
            "pregunta": cur["pregunta"],
            "opciones": dict(ops),
            "respuesta_correcta": cur["respuesta_correcta"],
            "explicacion": cur["explicacion"],
            "fuente": fuente,
            "tipo_fuente": "recopilacion",
            "anio": None,
            "pagina": None,
            "licencia": "",
            "es_original": False,
            "verificada": False,
            "duplicado": False,
            "estado": "aprobado",
            "figura_id": fig,
            "imagen_url": None,
            "imagen_path": m["archivo"],
            "requiere_imagen": False,  # autocontenida; la figura queda como apoyo
            "resoluble_texto": "SI",
            "_origen": f"restaurada_fase4:{fuente}:{tid}",
            "_tipo_gestion": "autocontenido_reformulado",
            "_aprobable": True,
            "_curaduria": "fase4_transcripcion_render_verificado",
        }
        filas.append(row)
        por_id[fid] = row
        rep["restauradas"] += 1
        print(f"  + {fid} restaurada -> aprobado ({cur['respuesta_correcta']})")

    estados = Counter(r.get("estado") for r in filas)
    print(f"promovidas={rep['promovidas']} estados={dict(estados)}")
    if dry:
        print("DRY-RUN.")
        return
    shutil.copy(BANCO, str(BANCO) + ".bak_fase3")
    BANCO.write_text("\n".join(json.dumps(r, ensure_ascii=False) for r in filas) + "\n",
                     encoding="utf-8")
    print("Banco actualizado (backup .bak_fase3).")


if __name__ == "__main__":
    main()

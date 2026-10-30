/**
 * Carga el banco fusionado (Fase 1) en Firestore (colección `questions`).
 *
 * Fuente por defecto: tmp/fase1/banco_fusionado.jsonl (generado por
 * scripts/merge_bank_fase1.py con IDs namespaced rec2_*, rec3_*...).
 * Acepta --source=<ruta> para otro JSONL con schema maestro o trazabilidad.
 *
 * Reglas aplicadas (coherentes con la app):
 *  - Se respeta `estado="deshabilitado"` del JSONL. Nunca se recalcula.
 *  - Para el resto, el estado se recalcula:
 *      "aprobado"            → enunciado + opciones A-D completas + respuesta
 *                               en A-D + no requiere imagen + respuesta
 *                               CONFIABLE (origen base/curada/restaurada,
 *                               `_curaduria` o verificada). `pendiente` nunca
 *                               se auto-promueve: las letras de trazabilidad
 *                               con `clave_disponible=NO` quedan pendientes.
 *      "aprobado_con_imagen" → aprobable + requiere_imagen + imagen_url
 *                               presente (la app descarga ambos estados).
 *      "pendiente"           → falta respuesta/enunciado/opciones, o requiere
 *                               imagen aún sin subir (Fase 3).
 *  - `opciones` acepta objeto {A-D} o lista [A,B,C,D]; se guarda como LISTA
 *    ordenada (asi la lee la app: QuestionRepository.documentToQuestion()).
 *  - IDs: se normalizan a `{ns}_{id}` corto (rec2_1, rec3_RL-0001), igual que
 *    merge_bank_fase1.py. Si el JSONL ya trae IDs namespaced, se respetan.
 *  - Escritura en batches de 450 (limite Firestore: 500).
 *  - Imagenes: si `imagen_disponible === 'SI'` y el archivo existe, se sube a
 *    Firebase Storage (ruta plana figuras/{archivo}, se omite si ya existe)
 *    y se guarda la URL pública en `imagen_url`. Un `imagen_url` ya presente
 *    en el JSONL se conserva.
 *
 * Uso:
 *   1) npm install firebase-admin
 *   2) Firebase Console -> Configuracion del proyecto -> Cuentas de servicio
 *      -> "Generar nueva clave privada" (guarda el JSON FUERA del repo)
 *   3) Pre-vuelo (sin escribir): node scripts/upload_bank_to_firestore.js --dry-run [--source=<jsonl>]
 *   4) Subida real: node scripts/upload_bank_to_firestore.js <ruta-al-serviceAccountKey.json> [--source=<jsonl>]
 *
 * Es idempotente: volver a ejecutarlo sobrescribe los documentos con el mismo id
 * y omite las imágenes que ya existen en Storage.
 */
const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const serviceAccountPath = args.find((a) => !a.startsWith('--'));

if (!dryRun && !serviceAccountPath) {
    console.error('Uso: node scripts/upload_bank_to_firestore.js [--dry-run] <serviceAccountKey.json>');
    process.exit(1);
}

// firebase-admin solo se carga en subida real (el dry-run no lo necesita).
// v14+ es modular: credenciales en 'firebase-admin/app', DB en 'firebase-admin/firestore'.
let db = null;
let firestoreFieldValue = null;
let storage = null;
if (!dryRun) {
    const { initializeApp, cert } = require('firebase-admin/app');
    const { getFirestore, FieldValue } = require('firebase-admin/firestore');
    const { getStorage } = require('firebase-admin/storage');
    initializeApp({
        credential: cert(require(path.resolve(serviceAccountPath))),
    });
    db = getFirestore();
    firestoreFieldValue = FieldValue;
    storage = getStorage();
}

const LETRAS = ['A', 'B', 'C', 'D'];

// Namespace corto por fuente (igual que scripts/merge_bank_fase1.py).
const NS_FUENTE = {
    'recopilacion-2': 'rec2',
    'recopilacion-3': 'rec3',
    'recopilacion-4': 'rec4',
    'recopilacion-6': 'rec6',
    'recopilacion-7': 'rec7',
};

function nid(fuente, idOrigen) {
    const f = String(fuente || '').trim();
    const oid = String(idOrigen || '').replace(/\s+/g, '');
    // Solo las recopilaciones usan namespace corto; el resto (base profe_alex_*,
    // RL-ORG-*, banco ya fusionado) conserva su ID tal cual.
    const ns = NS_FUENTE[f];
    if (!ns) return oid;
    if (oid.toLowerCase().startsWith(ns.toLowerCase() + '_')) return oid;
    return `${ns}_${oid}`;
}

function esAprobable(q) {
    const resp = String(q.respuesta_correcta || '').toUpperCase();
    const obj = normalizarOpciones(q.opciones);
    const opcionesCompletas = LETRAS.every((l) => obj[l] != null && String(obj[l]).trim() !== '');
    return opcionesCompletas && LETRAS.includes(resp) && String(q.pregunta || '').trim() !== '';
}

function requiereImagen(q) {
    const v = q.requiere_imagen;
    if (v === true) return true;
    return String(v || '').trim().toUpperCase() === 'SI';
}

/**
 * Respuesta CONFIABLE (Fase 5): solo estas filas pueden quedar `aprobado`.
 * Las filas de trazabilidad con letra pero `clave_disponible=NO` tienen
 * procedencia sin verificar: quedan `pendiente` para el panel admin aunque
 * cumplan el formato (evita mostrar respuestas posiblemente erroneas).
 */
function respuestaConfiable(q) {
    if (q.verificada === true || q._curaduria != null) return true;
    const origen = q._origen || '';
    return origen === '' || origen.startsWith('curada') || origen.startsWith('restaurada');
}

/**
 * Convierte opciones de lista ["a","b","c","d"] a objeto {A:"a",B:"b",C:"c",D:"d"}
 * Si ya es objeto, lo devuelve igual.
 */
function normalizarOpciones(opciones) {
    if (Array.isArray(opciones)) {
        const obj = {};
        LETRAS.forEach((l, i) => {
            if (opciones[i] != null && String(opciones[i]).trim() !== '') {
                obj[l] = String(opciones[i]).trim();
            }
        });
        return obj;
    }
    if (opciones && typeof opciones === 'object') {
        return opciones;
    }
    return {};
}

/**
 * Sube una imagen a Firebase Storage y devuelve la URL pública.
 * Ruta plana en Storage: figuras/{nombre_archivo} (varias preguntas pueden
 * compartir la misma figura FIG-XXX). Si ya existe, se reutiliza sin
 * re-subir: la subida es idempotente.
 */
async function subirImagen(bucket, imagenPath) {
    if (!imagenPath || !fs.existsSync(imagenPath)) {
        return null;
    }
    const nombreArchivo = path.basename(imagenPath);
    const rutaStorage = `figuras/${nombreArchivo}`;
    const archivo = bucket.file(rutaStorage);

    const [existe] = await archivo.exists();
    if (!existe) {
        await archivo.save(fs.readFileSync(imagenPath), {
            metadata: { contentType: 'image/png' },
        });
        await archivo.makePublic();
    }
    return `https://storage.googleapis.com/${bucket.name}/${rutaStorage}`;
}

async function main() {
    // Fuente: banco fusionado Fase 1 por defecto; --source=<ruta> lo override.
    // Acepta schema maestro o trazabilidad (buildDoc normaliza ambos).
    const sourceArg = args.find((a) => a.startsWith('--source='));
    const candidatos = sourceArg
        ? [path.resolve(sourceArg.slice('--source='.length))]
        : [
            path.join(__dirname, '..', 'tmp', 'fase1', 'banco_fusionado.jsonl'),
            path.join(__dirname, '..', '..', 'udea_dataset', 'dataset', 'preguntas_con_trazabilidad.jsonl'),
            path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'questions', 'questions.jsonl'),
        ];
    const jsonlPath = candidatos.find((p) => fs.existsSync(p));

    if (!jsonlPath) {
        console.error('ERROR: no se encontro ningun JSONL fuente.');
        console.error('Genera el banco con: python3 scripts/merge_bank_fase1.py');
        console.error('O pasa --source=<ruta-al-jsonl>');
        process.exit(1);
    }
    
    const lines = fs.readFileSync(jsonlPath, 'utf8').split('\n').filter((l) => l.trim());

    console.log(`Leyendo ${lines.length} preguntas de ${jsonlPath}`);

    // Validacion previa: JSON valido, ids presentes y unicos (namespaced).
    const vistos = new Set();
    const docs = [];
    for (let i = 0; i < lines.length; i++) {
        let q;
        try {
            q = JSON.parse(lines[i]);
        } catch (e) {
            throw new Error(`Linea ${i + 1}: JSON invalido: ${e.message}`);
        }
        if (!q.id || !String(q.id).trim()) {
            throw new Error(`Linea ${i + 1}: falta campo "id"`);
        }

        // ID namespaced corto (rec2_1, rec3_RL-0001), igual que Fase 1.
        // Si el JSONL ya viene fusionado, el id ya es final y se respeta.
        const idUnico = nid(q.fuente, q.id);

        if (vistos.has(idUnico)) {
            throw new Error(`ID duplicado: ${idUnico} (fuente: ${q.fuente}, id original: ${q.id})`);
        }
        vistos.add(idUnico);
        docs.push({ id: idUnico, data: buildDoc(q) });
    }

    const stats = { aprobado: 0, aprobado_con_imagen: 0, pendiente: 0, deshabilitado: 0, con_imagen: 0 };
    for (const d of docs) {
        stats[d.data.estado]++;
        if (d.data.imagen_disponible === 'SI') stats.con_imagen++;
    }

    console.log(`A enviar: ${docs.length} docs -> aprobados: ${stats.aprobado}, con imagen: ${stats.aprobado_con_imagen}, pendientes: ${stats.pendiente}, deshabilitados: ${stats.deshabilitado}`);

    if (dryRun) {
        console.log('DRY-RUN: no se escribio nada en Firestore.');
        // Muestra casos borde para auditar.
        for (const d of docs.filter((x) => x.data.estado === 'pendiente').slice(0, 5)) {
            console.log(`  pendiente ej: ${d.id} opciones=${d.data.opciones.length} resp='${d.data.respuesta_correcta}'`);
        }
        // Muestra ejemplos que requieren imagen (Fase 3 los promovera).
        const conImg = docs.filter((x) => x.data.requiere_imagen === 'SI').slice(0, 3);
        for (const d of conImg) {
            console.log(`  requiere imagen: ${d.id} estado=${d.data.estado} url='${d.data.imagen_url}'`);
        }
        return;
    }

    // Subir imágenes a Storage y construir documentos
    const bucket = storage.bucket();
    let imagenesSubidas = 0;
    let imagenesFallidas = 0;

    console.log('Subiendo imágenes a Firebase Storage...');
    for (const d of docs) {
        if (d.data.imagen_disponible === 'SI' && d.data.imagen_path && !d.data.imagen_url) {
            try {
                const rutaImagen = path.join(__dirname, '..', '..', d.data.imagen_path);
                const urlPublica = await subirImagen(bucket, rutaImagen);
                if (urlPublica) {
                    d.data.imagen_url = urlPublica;
                    imagenesSubidas++;
                    // Promueve a aprobado_con_imagen si ya es aprobable.
                    if (d.data.opciones.length === 4 && LETRAS.includes(d.data.respuesta_correcta)) {
                        d.data.estado = 'aprobado_con_imagen';
                    }
                    console.log(`  ✓ ${d.id}: ${urlPublica}`);
                } else {
                    console.warn(`  ⚠ ${d.id}: no se encontro la imagen en ${rutaImagen}`);
                    imagenesFallidas++;
                }
            } catch (e) {
                console.error(`  ✗ ${d.id}: error subiendo imagen: ${e.message}`);
                imagenesFallidas++;
            }
        }
    }
    console.log(`Imágenes subidas: ${imagenesSubidas}, fallidas: ${imagenesFallidas}`);

    // Escribir documentos en Firestore
    let batch = db.batch();
    let escritas = 0;

    for (const d of docs) {
        batch.set(
            db.collection('questions').doc(d.id),
            { ...d.data, fechaActualizacion: firestoreFieldValue.serverTimestamp() }
        );
        escritas++;

        if (escritas % 450 === 0) {
            await batch.commit();
            batch = db.batch();
        }
    }

    await batch.commit();
    const finales = { aprobado: 0, aprobado_con_imagen: 0, pendiente: 0, deshabilitado: 0 };
    for (const d of docs) finales[d.data.estado]++;
    console.log(
        `Cargadas ${escritas} preguntas -> aprobadas: ${finales.aprobado}, con imagen: ${finales.aprobado_con_imagen}, pendientes: ${finales.pendiente}, deshabilitadas: ${finales.deshabilitado}`
    );
    console.log(`Imágenes subidas a Storage: ${imagenesSubidas}`);
    console.log('  La app descarga estado=aprobado y aprobado_con_imagen.');
}

function buildDoc(q) {
    const tieneUrl = q.imagen_url != null && String(q.imagen_url).trim() !== '';
    const aprobable = esAprobable(q);
    const conImagen = requiereImagen(q);
    const confiable = respuestaConfiable(q);

    // Respeta el curado manual: deshabilitado nunca se recalcula.
    // `pendiente` nunca se auto-promueve (Fase 5): la promocion requiere
    // curaduria (panel admin o scripts fase4). Solo se puede perder `aprobado`
    // si la fila dejo de ser valida.
    let estado = 'pendiente';
    if (q.estado === 'deshabilitado') {
        estado = 'deshabilitado';
    } else if (aprobable && !conImagen && confiable) {
        estado = 'aprobado';
    } else if (aprobable && conImagen && tieneUrl && confiable) {
        estado = 'aprobado_con_imagen';
    }
    // Casos a pendiente: no aprobable; requiere imagen sin URL;
    // respuesta no confiable (trazabilidad sin verificar).

    // Normalizar opciones: de lista a objeto A/B/C/D
    const opcionesObj = normalizarOpciones(q.opciones);
    
    // Lista ordenada A-D (formato que espera documentToQuestion()).
    // Se descartan nulos Y cadenas vacias para no guardar ["","","",""].
    const opciones = LETRAS
        .map((l) => (opcionesObj[l] != null ? String(opcionesObj[l]).trim() : ''))
        .filter((v) => v !== '');

    return {
        // Campos del dataset original (compatibilidad)
        area: q.area || q.seccion || '',
        subtema: q.subtema || '',
        componente: q.componente || (q.seccion === 'RL' ? 'Razonamiento Lógico' : 'Competencia Lectora'),
        competencia: q.competencia || '',
        tipo_texto: q.tipo_texto || '',
        dificultad: q.dificultad || 'media',
        texto_base: q.texto_base || '',
        contexto: q.contexto || q.descripcion_contexto || '',
        pregunta: q.pregunta || '',
        opciones,
        respuesta_correcta: String(q.respuesta_correcta || '').toUpperCase(),
        explicacion: q.explicacion || '',
        fuente: q.fuente || q.pdf_origen || '',
        tipo_fuente: q.tipo_fuente || '',
        anio: q.anio ?? null,
        pagina: q.pagina ?? null,
        licencia: q.licencia || '',
        es_original: q.es_original === true,
        verificada: q.verificada === true,
        duplicado: q.duplicado === true,
        estado,
        
        // Campos nuevos del dataset con trazabilidad
        imagen_disponible: q.imagen_disponible || 'NO',
        imagen_path: q.imagen_path || '',
        imagen_url: tieneUrl ? String(q.imagen_url).trim() : '', // Se llena al subir a Storage
        figura_id: q.figura_id || '',
        requiere_imagen: q.requiere_imagen || (conImagen ? 'SI' : 'NO'),
        bbox: q.bbox || '',
        resoluble_texto: q.resoluble_texto || '',
        tipo_contexto: q.tipo_contexto || '',
        descripcion_contexto: q.descripcion_contexto || '',
        clave_disponible: q.clave_disponible || '',
        fuente_respuesta: q.fuente_respuesta || '',
        tipo_gestion: q.tipo_gestion || '',
        prioridad: q.prioridad || '',
        accion_recomendada: q.accion_recomendada || '',
        estado_propuesto: q.estado_propuesto || '',
        numero: q.numero || '',
    };
}

main().catch((e) => {
    console.error('ERROR:', e.message);
    process.exit(1);
});

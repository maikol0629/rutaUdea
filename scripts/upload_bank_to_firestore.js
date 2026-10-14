/**
 * Carga el banco maestro de preguntas en Firestore (colección `questions`)
 * desde el JSONL corregido del asset: app/src/main/assets/questions/questions.jsonl
 *
 * Reglas aplicadas (coherentes con la app):
 *  - Se respeta `estado="deshabilitado"` del JSONL (limpieza v4: preguntas que
 *    referencian figuras/tablas inexistentes). Nunca se recalcula.
 *  - Para el resto, el estado se recalcula:
 *      "aprobado"  → tiene enunciado, opciones A-D completas y respuesta en A-D
 *                     (la app solo descarga aprobadas: updateBankFromFirestore)
 *      "pendiente" → falta respuesta/enunciado/opciones;
 *                     queda registrada para validar desde el futuro panel admin
 *  - `opciones` se guarda como LISTA ordenada [A, B, C, D] (asi la lee la app:
 *    QuestionRepository.documentToQuestion()). Las opciones vacias se descartan.
 *  - Escritura en batches de 450 (limite Firestore: 500).
 *
 * Uso:
 *   1) npm install firebase-admin
 *   2) Firebase Console -> Configuracion del proyecto -> Cuentas de servicio
 *      -> "Generar nueva clave privada" (guarda el JSON FUERA del repo)
 *   3) Pre-vuelo (sin escribir): node scripts/upload_bank_to_firestore.js --dry-run
 *   4) Subida real: node scripts/upload_bank_to_firestore.js <ruta-al-serviceAccountKey.json>
 *
 * Es idempotente: volver a ejecutarlo sobrescribe los documentos con el mismo id.
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
if (!dryRun) {
    const { initializeApp, cert } = require('firebase-admin/app');
    const { getFirestore, FieldValue } = require('firebase-admin/firestore');
    initializeApp({
        credential: cert(require(path.resolve(serviceAccountPath))),
    });
    db = getFirestore();
    firestoreFieldValue = FieldValue;
}

const LETRAS = ['A', 'B', 'C', 'D'];

function esAprobable(q) {
    const resp = String(q.respuesta_correcta || '').toUpperCase();
    const opcionesCompletas =
        q.opciones && LETRAS.every((l) => q.opciones[l] != null && String(q.opciones[l]).trim() !== '');
    return opcionesCompletas && LETRAS.includes(resp) && String(q.pregunta || '').trim() !== '';
}

function buildDoc(q) {
    // Respeta el curado manual: deshabilitado nunca se recalcula.
    const estado = q.estado === 'deshabilitado'
        ? 'deshabilitado'
        : (esAprobable(q) ? 'aprobado' : 'pendiente');

    // Lista ordenada A-D (formato que espera documentToQuestion()).
    // Se descartan nulos Y cadenas vacias para no guardar ["","","",""].
    const opciones = LETRAS
        .map((l) => (q.opciones && q.opciones[l] != null ? String(q.opciones[l]).trim() : ''))
        .filter((v) => v !== '');

    return {
        area: q.area || '',
        subtema: q.subtema || '',
        componente: q.componente
            || (q.area === 'razonamiento_logico' ? 'Razonamiento Lógico' : 'Competencia Lectora'),
        competencia: q.competencia || '',
        tipo_texto: q.tipo_texto || '',
        dificultad: q.dificultad || 'media',
        texto_base: q.texto_base || '',
        contexto: q.contexto || '',
        pregunta: q.pregunta || '',
        opciones,
        respuesta_correcta: String(q.respuesta_correcta || '').toUpperCase(),
        explicacion: q.explicacion || '',
        fuente: q.fuente || '',
        tipo_fuente: q.tipo_fuente || '',
        anio: q.anio ?? null,
        pagina: q.pagina ?? null,
        licencia: q.licencia || '',
        es_original: q.es_original === true,
        verificada: q.verificada === true,
        duplicado: q.duplicado === true,
        estado,
    };
}

async function main() {
    const jsonlPath = path.join(
        __dirname, '..', 'app', 'src', 'main', 'assets', 'questions', 'questions.jsonl'
    );
    const lines = fs.readFileSync(jsonlPath, 'utf8').split('\n').filter((l) => l.trim());

    console.log(`Leyendo ${lines.length} preguntas de ${jsonlPath}`);

    // Validacion previa: JSON valido, ids presentes y unicos.
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
        if (vistos.has(String(q.id))) {
            throw new Error(`ID duplicado: ${q.id}`);
        }
        vistos.add(String(q.id));
        docs.push({ id: String(q.id), data: buildDoc(q) });
    }

    const stats = { aprobado: 0, pendiente: 0, deshabilitado: 0 };
    for (const d of docs) stats[d.data.estado]++;

    console.log(`A enviar: ${docs.length} docs -> aprobados: ${stats.aprobado}, pendientes: ${stats.pendiente}, deshabilitados: ${stats.deshabilitado}`);

    if (dryRun) {
        console.log('DRY-RUN: no se escribio nada en Firestore.');
        // Muestra casos borde para auditar.
        for (const d of docs.filter((x) => x.data.estado === 'pendiente').slice(0, 5)) {
            console.log(`  pendiente ej: ${d.id} opciones=${d.data.opciones.length} resp='${d.data.respuesta_correcta}'`);
        }
        return;
    }

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
    console.log(
        `Cargadas ${escritas} preguntas -> aprobadas: ${stats.aprobado}, pendientes: ${stats.pendiente}, deshabilitadas: ${stats.deshabilitado}`
    );
    console.log('  Solo estado=aprobado se descarga en la app.');
}

main().catch((e) => {
    console.error('ERROR:', e.message);
    process.exit(1);
});

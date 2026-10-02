/**
 * Carga el banco maestro de preguntas en Firestore (colección `questions`)
 * desde el JSONL corregido del asset: app/src/main/assets/questions/questions.jsonl
 *
 * Reglas aplicadas (coherentes con la app):
 *  - Cada pregunta es un documento con id = q.id y estado:
 *      "aprobado"  → tiene enunciado, opciones A–D completas y respuesta en A–D
 *                     (la app solo descarga aprobadas: updateBankFromFirestore)
 *      "pendiente"  → falta respuesta/enunciado/opciones (32 RL hoy);
 *                     queda registrada para validar desde el futuro panel admin
 *  - `opciones` se guarda como LISTA ordenada [A, B, C, D] (así la lee la app:
 *    QuestionRepository.documentToQuestion()).
 *  - Escritura en batches de 450 (límite Firestore: 500).
 *
 * Uso:
 *   1) npm install firebase-admin
 *   2) Firebase Console → ⚙️ Configuración del proyecto → Cuentas de servicio
 *      → "Generar nueva clave privada" (guarda el JSON)
 *   3) node scripts/upload_bank_to_firestore.js <ruta-al-serviceAccountKey.json>
 *
 * Es idempotente: volver a ejecutarlo sobrescribe los documentos con el mismo id.
 */
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccountPath = process.argv[2];
if (!serviceAccountPath) {
    console.error('Uso: node scripts/upload_bank_to_firestore.js <serviceAccountKey.json>');
    process.exit(1);
}

admin.initializeApp({
    credential: admin.credential.cert(require(path.resolve(serviceAccountPath))),
});
const db = admin.firestore();

const LETRAS = ['A', 'B', 'C', 'D'];

function esAprobable(q) {
    const resp = String(q.respuesta_correcta || '').toUpperCase();
    const opcionesCompletas =
        q.opciones && LETRAS.every((l) => q.opciones[l] != null && String(q.opciones[l]).trim() !== '');
    return opcionesCompletas && LETRAS.includes(resp) && String(q.pregunta || '').trim() !== '';
}

async function main() {
    const jsonlPath = path.join(
        __dirname, '..', 'app', 'src', 'main', 'assets', 'questions', 'questions.jsonl'
    );
    const lines = fs.readFileSync(jsonlPath, 'utf8').split('\n').filter((l) => l.trim());

    console.log(`Leyendo ${lines.length} preguntas de ${jsonlPath}`);
    let batch = db.batch();
    let escritas = 0;
    const stats = { aprobado: 0, pendiente: 0 };

    for (const line of lines) {
        const q = JSON.parse(line);

        // Lista ordenada A–D (formato que espera documentToQuestion()).
        const opciones = LETRAS
            .map((l) => (q.opciones && q.opciones[l] != null ? String(q.opciones[l]) : null))
            .filter((v) => v != null);

        const estado = esAprobable(q) ? 'aprobado' : 'pendiente';
        stats[estado]++;

        batch.set(db.collection('questions').doc(String(q.id)), {
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
            fechaActualizacion: admin.firestore.FieldValue.serverTimestamp(),
        });
        escritas++;

        if (escritas % 450 === 0) {
            await batch.commit();
            batch = db.batch();
        }
    }

    await batch.commit();
    console.log(
        `✔ Cargadas ${escritas} preguntas → aprobadas: ${stats.aprobado}, pendientes: ${stats.pendiente}`
    );
    console.log('  Las pendientes NO se descargan en la app (solo estado=aprobado).');
}

main().catch((e) => {
    console.error('ERROR:', e);
    process.exit(1);
});

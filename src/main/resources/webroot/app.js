// ── Utilidades ──

function showLoading(id) {
    document.getElementById(id + 'Loading').classList.remove('hidden');
    document.getElementById(id + 'Result').classList.add('hidden');
    document.getElementById(id + 'Error').classList.add('hidden');
}

function showResult(id, message) {
    document.getElementById(id + 'Loading').classList.add('hidden');
    document.getElementById(id + 'Result').classList.remove('hidden');
    document.getElementById(id + 'Result').innerHTML = message;
    document.getElementById(id + 'Error').classList.add('hidden');
}

function showError(id, message) {
    document.getElementById(id + 'Loading').classList.add('hidden');
    document.getElementById(id + 'Result').classList.add('hidden');
    document.getElementById(id + 'Error').classList.remove('hidden');
    document.getElementById(id + 'Error').innerHTML = message;
}

// ── Saludo ──

function greet() {
    const name = document.getElementById('greetName').value.trim();

    if (!name) {
        showError('greet', 'Por favor ingresa un nombre.');
        return;
    }

    showLoading('greet');

    fetch('/hello?name=' + encodeURIComponent(name))
        .then(response => {
            if (!response.ok) throw new Error('Error: ' + response.status);
            return response.text();
        })
        .then(data => showResult('greet', '🎉 ' + data))
        .catch(error => showError('greet', '❌ ' + error.message));
}

// ── Cuadrado ──

function square() {
    const value = document.getElementById('squareValue').value.trim();

    if (!value) {
        showError('square', 'Por favor ingresa un número.');
        return;
    }

    showLoading('square');

    fetch('/square?value=' + encodeURIComponent(value))
        .then(response => {
            if (!response.ok) throw new Error('Error: ' + response.status);
            return response.json();
        })
        .then(data => showResult('square', '📐 ' + data.input + '² = ' + data.square))
        .catch(error => showError('square', '❌ ' + error.message));
}

// ── PI ──

function getPi() {
    showLoading('pi');

    fetch('/pi')
        .then(response => {
            if (!response.ok) throw new Error('Error: ' + response.status);
            return response.text();
        })
        .then(data => showResult('pi', '🥧 π = ' + data))
        .catch(error => showError('pi', '❌ ' + error.message));
}

// ── Hora del servidor ──

function getTime() {
    showLoading('time');

    fetch('/time')
        .then(response => {
            if (!response.ok) throw new Error('Error: ' + response.status);
            return response.json();
        })
        .then(data => showResult('time', '🕐 ' + data.serverTime))
        .catch(error => showError('time', '❌ ' + error.message));
}
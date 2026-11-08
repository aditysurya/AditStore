<!doctype html>
<html lang="id">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Login | Products API</title>
    <style>
        :root {
            color-scheme: light;
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            color: #172033;
            background: #eef2f7;
        }
        * { box-sizing: border-box; }
        body { margin: 0; min-height: 100vh; display: grid; place-items: center; padding: 24px; }
        main { width: min(100%, 440px); background: #fff; border: 1px solid #dce3ed; border-radius: 12px; padding: 32px; box-shadow: 0 16px 40px rgb(23 32 51 / 10%); }
        h1 { margin: 0 0 8px; font-size: 1.7rem; }
        p { color: #5d687b; line-height: 1.5; }
        .qr { min-height: 256px; display: grid; place-items: center; margin: 24px 0; }
        .qr svg { display: block; width: 256px; height: 256px; max-width: 100%; }
        dl { display: grid; grid-template-columns: auto 1fr; gap: 8px 16px; margin: 0 0 24px; font-size: .9rem; }
        dt { color: #69758a; }
        dd { margin: 0; overflow-wrap: anywhere; }
        label { display: block; margin-bottom: 8px; font-weight: 600; }
        input, button { width: 100%; min-height: 44px; border-radius: 8px; font: inherit; }
        input { border: 1px solid #c9d2df; padding: 8px 12px; letter-spacing: .2em; }
        button { margin-top: 12px; border: 0; color: #fff; background: #1d5fd1; cursor: pointer; font-weight: 600; }
        button:disabled { cursor: wait; opacity: .65; }
        #status { min-height: 24px; margin: 12px 0 0; font-size: .9rem; }
        .success { color: #18764a; }
        .error { color: #b42318; }
        .hidden { display: none; }
    </style>
</head>
<body>
    <main>
        <h1>Login</h1>
        <p>Scan QR code dengan Google Authenticator, lalu masukkan kode 6 digit.</p>

        <div id="loading">Memuat konfigurasi login...</div>
        <section id="login-content" class="hidden" aria-live="polite">
            <div class="qr" id="qr-code" aria-label="QR code konfigurasi authenticator"></div>
            <dl>
                <dt>Issuer</dt><dd id="issuer"></dd>
                <dt>Akun</dt><dd id="account"></dd>
                <dt>Secret</dt><dd id="secret"></dd>
            </dl>
            <form id="login-form">
                <label for="code">Kode authenticator</label>
                <input id="code" name="code" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" required>
                <button type="submit">Login</button>
            </form>
            <p id="status" role="status"></p>
        </section>
    </main>

    <script>
        const loading = document.getElementById('loading');
        const content = document.getElementById('login-content');
        const status = document.getElementById('status');
        const form = document.getElementById('login-form');

        function showStatus(message, type = '') {
            status.textContent = message;
            status.className = type;
        }

        async function loadLoginData() {
            const response = await fetch('/login', { headers: { Accept: 'application/json' } });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'Konfigurasi login gagal dimuat.');

            document.getElementById('qr-code').innerHTML = data.qr_code || '';
            document.getElementById('issuer').textContent = data.issuer || '-';
            document.getElementById('account').textContent = data.account || '-';
            document.getElementById('secret').textContent = data.secret || '-';
            loading.classList.add('hidden');
            content.classList.remove('hidden');
        }

        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const button = form.querySelector('button');
            button.disabled = true;
            showStatus('Memverifikasi...');
            try {
                const response = await fetch('/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                    body: JSON.stringify({ code: document.getElementById('code').value })
                });
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || 'Login gagal.');
                showStatus(data.message || 'Login berhasil.', 'success');
            } catch (error) {
                showStatus(error.message, 'error');
            } finally {
                button.disabled = false;
            }
        });

        loadLoginData().catch((error) => {
            loading.textContent = error.message;
            loading.className = 'error';
        });
    </script>
</body>
</html>
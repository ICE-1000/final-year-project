document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('deptRegisterForm');
    if (!form) return;
    const message = document.getElementById('message');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            departmentName: document.getElementById('deptName').value.trim(),
            departmentCode: document.getElementById('deptCode').value.trim(),
            username: document.getElementById('username').value.trim(),
            email: document.getElementById('email').value.trim(),
            password: document.getElementById('password').value
        };

        try {
            const res = await fetch(`${API_BASE}/auth/department/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                let errMsg = 'Registration failed';
                try {
                    const err = await res.json();
                    errMsg = err.message || errMsg;
                } catch (e) {
                    errMsg = `${res.status} ${res.statusText}`;
                }
                throw new Error(errMsg);
            }

            if (message) {
                message.style.color = 'green';
                message.textContent = 'request submitted waiting for admin approval';
            }
            form.reset();
        } catch (err) {
            if (message) {
                message.style.color = 'red';
                message.textContent = err.message || 'Registration error';
            }
        }
    });
});

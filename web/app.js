// 后端 API 基地址，前端与后端不同源时需要绝对地址
const API_BASE = 'http://localhost:8080';
// 设备类型，与登录、刷新、登出接口保持一致
const DEVICE_TYPE = 'web';
const PAGE_SIZE = 10;

const TOKEN_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';
const NICKNAME_KEY = 'nickname';

// 统一请求函数：自动附带 AccessToken，401 时自动刷新并重发一次
async function request(url, method = 'GET', body = null) {
    const doFetch = (token) => fetch(API_BASE + url, {
        method,
        headers: buildHeaders(token, body),
        body: body === null ? undefined : JSON.stringify(body)
    });

    let res = await doFetch(localStorage.getItem(TOKEN_KEY));

    if (res.status === 401) {
        const refreshed = await refreshToken();
        if (!refreshed) {
            redirectToLogin();
            throw new Error('登录已失效，请重新登录');
        }
        // 使用新的 AccessToken 重发原请求
        res = await doFetch(localStorage.getItem(TOKEN_KEY));
        if (res.status === 401) {
            redirectToLogin();
            throw new Error('登录已失效，请重新登录');
        }
    }

    const data = await parseJson(res);
    if (!res.ok || (data && data.code !== 200)) {
        throw new Error((data && data.msg) || `请求失败(${res.status})`);
    }
    return data ? data.data : null;
}

function buildHeaders(token, body) {
    const headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    if (body !== null) {
        headers['Content-Type'] = 'application/json';
    }
    return headers;
}

async function parseJson(res) {
    const text = await res.text();
    if (!text) {
        return null;
    }
    try {
        return JSON.parse(text);
    } catch (e) {
        return null;
    }
}

// 刷新令牌，并发请求时复用同一个刷新过程，避免 Token 轮换导致互相顶掉
let refreshing = null;

function refreshToken() {
    if (refreshing) {
        return refreshing;
    }
    refreshing = (async () => {
        const refresh = localStorage.getItem(REFRESH_KEY);
        if (!refresh) {
            return false;
        }
        try {
            const res = await fetch(API_BASE + '/api/v1/auth/refresh', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({refreshToken: refresh, deviceType: DEVICE_TYPE})
            });
            const data = await parseJson(res);
            if (!res.ok || !data || data.code !== 200 || !data.data) {
                return false;
            }
            // 刷新接口会轮换 RefreshToken，必须替换本地旧值
            localStorage.setItem(TOKEN_KEY, data.data.accessToken);
            localStorage.setItem(REFRESH_KEY, data.data.refreshToken);
            return true;
        } catch (e) {
            return false;
        } finally {
            refreshing = null;
        }
    })();
    return refreshing;
}

function redirectToLogin() {
    clearAuth();
    location.href = 'login.html';
}

function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(NICKNAME_KEY);
}

function showMsg(text, isError = true) {
    const el = document.getElementById('msg');
    if (!el) {
        return;
    }
    el.textContent = text;
    el.hidden = false;
    el.classList.toggle('error', isError);
    el.classList.toggle('success', !isError);
}

function hideMsg() {
    const el = document.getElementById('msg');
    if (el) {
        el.hidden = true;
    }
}

// ---------------- 登录页 ----------------

function initLoginPage() {
    // 已登录直接进入首页
    if (localStorage.getItem(TOKEN_KEY)) {
        location.href = 'index.html';
        return;
    }
    const form = document.getElementById('loginForm');
    const btn = document.getElementById('submitBtn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideMsg();
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        if (!username || !password) {
            showMsg('请输入用户名和密码');
            return;
        }
        btn.disabled = true;
        btn.textContent = '登录中...';
        try {
            const res = await fetch(API_BASE + '/api/v1/auth/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({username, password, deviceType: DEVICE_TYPE})
            });
            const data = await parseJson(res);
            if (!res.ok || !data || data.code !== 200 || !data.data) {
                showMsg((data && data.msg) || '登录失败');
                return;
            }
            localStorage.setItem(TOKEN_KEY, data.data.accessToken);
            localStorage.setItem(REFRESH_KEY, data.data.refreshToken);
            localStorage.setItem(NICKNAME_KEY, data.data.nickname || username);
            location.href = 'index.html';
        } catch (err) {
            showMsg('网络异常，请确认后端已启动');
        } finally {
            btn.disabled = false;
            btn.textContent = '登录';
        }
    });
}

// ---------------- 首页 ----------------

const state = {
    page: 1,
    total: 0
};

function initIndexPage() {
    if (!localStorage.getItem(TOKEN_KEY)) {
        redirectToLogin();
        return;
    }
    document.getElementById('nickname').textContent = localStorage.getItem(NICKNAME_KEY) || '用户';
    document.getElementById('billDate').value = today();
    document.getElementById('billForm').addEventListener('submit', handleCreate);
    document.getElementById('prevBtn').addEventListener('click', () => goPage(state.page - 1));
    document.getElementById('nextBtn').addEventListener('click', () => goPage(state.page + 1));
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);
    loadBills();
}

function today() {
    const d = new Date();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${m}-${day}`;
}

async function loadBills() {
    try {
        const data = await request(`/api/v1/bills?page=${state.page}&size=${PAGE_SIZE}`, 'GET');
        state.total = Number(data.total || 0);
        renderBills(data.records || []);
        renderPager();
    } catch (e) {
        showMsg(e.message);
    }
}

function renderBills(records) {
    const tbody = document.getElementById('billBody');
    if (!records.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty">暂无账单，先新增一条吧</td></tr>';
        return;
    }
    tbody.innerHTML = records.map((b) => {
        const isIncome = b.type === 2;
        const typeText = isIncome ? '收入' : '支出';
        const amount = Number(b.amount || 0).toFixed(2);
        return `<tr>
            <td>${escapeHtml(b.billDate || '')}</td>
            <td><span class="tag ${isIncome ? 'income' : 'expense'}">${typeText}</span></td>
            <td class="num ${isIncome ? 'income-text' : 'expense-text'}">${isIncome ? '+' : '-'}${amount}</td>
            <td>${escapeHtml(b.remark || '-')}</td>
        </tr>`;
    }).join('');
}

function renderPager() {
    const totalPages = Math.max(1, Math.ceil(state.total / PAGE_SIZE));
    document.getElementById('pageInfo').textContent = `第 ${state.page} / ${totalPages} 页`;
    document.getElementById('totalInfo').textContent = `共 ${state.total} 条`;
    document.getElementById('prevBtn').disabled = state.page <= 1;
    document.getElementById('nextBtn').disabled = state.page >= totalPages;
}

function goPage(page) {
    const totalPages = Math.max(1, Math.ceil(state.total / PAGE_SIZE));
    if (page < 1 || page > totalPages) {
        return;
    }
    state.page = page;
    loadBills();
}

async function handleCreate(e) {
    e.preventDefault();
    hideMsg();
    const amountRaw = document.getElementById('amount').value;
    const categoryId = document.getElementById('categoryId').value;
    const type = Number(document.getElementById('type').value);
    const billDate = document.getElementById('billDate').value;
    const remark = document.getElementById('remark').value.trim();

    const amount = Number(amountRaw);
    if (!amountRaw || isNaN(amount) || amount <= 0) {
        showMsg('请输入有效金额');
        return;
    }
    if (!categoryId || Number(categoryId) <= 0) {
        showMsg('请输入有效分类ID');
        return;
    }
    if (!billDate) {
        showMsg('请选择日期');
        return;
    }

    const btn = e.target.querySelector('button[type="submit"]');
    btn.disabled = true;
    try {
        await request('/api/v1/bills', 'POST', {
            categoryId: Number(categoryId),
            amount,
            type,
            billDate,
            remark: remark || null
        });
        document.getElementById('billForm').reset();
        document.getElementById('billDate').value = today();
        showMsg('新增成功', false);
        state.page = 1;
        await loadBills();
    } catch (err) {
        showMsg(err.message);
    } finally {
        btn.disabled = false;
    }
}

async function handleLogout() {
    const refresh = localStorage.getItem(REFRESH_KEY);
    if (refresh) {
        try {
            await fetch(API_BASE + '/api/v1/auth/logout', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({refreshToken: refresh, deviceType: DEVICE_TYPE})
            });
        } catch (e) {
            // 登出接口失败不影响本地清理
        }
    }
    redirectToLogin();
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// ---------------- 入口 ----------------

document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('loginForm')) {
        initLoginPage();
    } else if (document.getElementById('billForm')) {
        initIndexPage();
    }
});

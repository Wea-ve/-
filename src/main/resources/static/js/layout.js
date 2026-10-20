// ===== 侧边栏分组菜单折叠（当前页所在分组默认已通过 .open 展开，这里处理手动点击） =====
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.menu-group .group-title').forEach(function (title) {
        title.addEventListener('click', function () {
            title.parentElement.classList.toggle('open');
        });
    });

    // 没有可返回的浏览历史时，隐藏顶部返回按钮
    if (window.history.length <= 1) {
        document.querySelectorAll('.back-btn').forEach(function (btn) {
            btn.style.display = 'none';
        });
    }

    initClock();
    initDropdowns();
    initPageLoader();
    initDrawer();
});

// ===== 返回上一页（顶部面包屑栏的"← 返回"） =====
function goBack() {
    if (window.history.length > 1) {
        window.history.back();
    }
}

// ===== 顶栏实时时钟 =====
function initClock() {
    var el = document.getElementById('topbarClock');
    if (!el) return;
    function tick() {
        var now = new Date();
        var week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()];
        var pad = function (n) { return n < 10 ? '0' + n : n; };
        el.textContent = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
            + ' 周' + week + ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
    }
    tick();
    setInterval(tick, 1000);
}

// ===== 顶栏下拉（铃铛通知 / 头像菜单） =====
function initDropdowns() {
    var bellBtn = document.getElementById('bellBtn');
    var bellPanel = document.getElementById('bellPanel');
    var userBtn = document.getElementById('userMenuBtn');
    var userMenu = document.getElementById('userMenu');

    function closeAll() {
        if (bellPanel) bellPanel.classList.remove('open');
        if (userMenu) userMenu.classList.remove('open');
        if (bellBtn) bellBtn.classList.remove('active');
        if (userBtn) userBtn.classList.remove('active');
    }

    if (bellBtn && bellPanel) {
        bellBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            var willOpen = !bellPanel.classList.contains('open');
            closeAll();
            if (willOpen) {
                bellPanel.classList.add('open');
                bellBtn.classList.add('active');
                // 铃铛摇一摇
                bellBtn.classList.remove('shake');
                void bellBtn.offsetWidth;
                bellBtn.classList.add('shake');
            }
        });
    }

    if (userBtn && userMenu) {
        userBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            var willOpen = !userMenu.classList.contains('open');
            closeAll();
            if (willOpen) {
                userMenu.classList.add('open');
                userBtn.classList.add('active');
            }
        });
    }

    // 点击其他区域关闭
    document.addEventListener('click', closeAll);
}

// ===== 隐藏式抽屉菜单开关（桌面/手机通用） =====
function toggleSidebar() {
    document.body.classList.toggle('sidebar-open');
}

function initDrawer() {
    // 点击菜单链接后自动收起抽屉
    document.querySelectorAll('.sidebar .menu a').forEach(function (a) {
        a.addEventListener('click', function () {
            document.body.classList.remove('sidebar-open');
        });
    });
}

// ===== 顶部页面加载进度条（纯前端模拟，切页时触发） =====
function initPageLoader() {
    // 创建进度条元素
    var bar = document.createElement('div');
    bar.id = 'pageLoader';
    document.body.appendChild(bar);

    // 页面加载完成后快速跑满并淡出
    requestAnimationFrame(function () {
        bar.classList.add('run');
        setTimeout(function () {
            bar.classList.add('done');
            setTimeout(function () { bar.remove(); }, 400);
        }, 350);
    });

    // 点击站内链接时立即显示进度条（新页面加载时重新触发）
    document.addEventListener('click', function (e) {
        var a = e.target.closest('a[href]');
        if (a && a.getAttribute('href').charAt(0) === '/' && a.getAttribute('target') !== '_blank'
            && a.getAttribute('href').indexOf('javascript:') !== 0) {
            var loader = document.createElement('div');
            loader.id = 'pageLoader';
            loader.classList.add('running');
            document.body.appendChild(loader);
            setTimeout(function () { if (loader.parentNode) loader.remove(); }, 3000);
        }
    });
}

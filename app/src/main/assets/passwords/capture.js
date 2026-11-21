(function () {
    if (window.__VIA_AUTO_FILL) return;
    window.__VIA_AUTO_FILL = true;
    var secret = __VIA_PASSWORD_SECRET__;
    var focusTimer;
    var lastCapture = 0;
    var loginClasses = ['login', 'log-in', 'log_in', 'signin', 'sign-in', 'sign_in', 'signup', 'sign-up', 'sign_up'];
    var loginWords = ['login', 'signin', 'signup', '登录', '登陆', '注册', '登入'];

    function send(action, values) {
        var message = values || {};
        message.action = action;
        window.via.postMessage(secret, JSON.stringify(message));
    }
    function visibility(show, immediate) {
        clearTimeout(focusTimer);
        if (immediate) { try { send(109, {show: show ? 1 : 0}); } catch (ignored) {} }
        else focusTimer = setTimeout(function () { try { send(109, {show: show ? 1 : 0}); } catch (ignored) {} }, 50);
    }
    function throttled() { return Date.now() - lastCapture < 500; }
    function nearestPassword(target) {
        if (!target) return null;
        if (target.tagName === 'INPUT' && target.type === 'password') return target;
        if (typeof target.getBoundingClientRect !== 'function') return null;
        var bounds = target.getBoundingClientRect();
        if (bounds.width <= 0 || bounds.height <= 0) return null;
        var inputs = document.getElementsByTagName('input');
        var best = null;
        var bestDistance = Number.MAX_VALUE;
        for (var index = 0; index < inputs.length; index++) {
            var input = inputs[index];
            if (input.type !== 'password') continue;
            var candidate = input.getBoundingClientRect();
            if (candidate.width <= 0 || candidate.height <= 0) continue;
            var dx = Math.abs((bounds.left + bounds.right) / 2 - (candidate.left + candidate.right) / 2);
            var dy = Math.abs((bounds.top + bounds.bottom) / 2 - (candidate.top + candidate.bottom) / 2);
            if (dx > 260 || dy > 320) continue;
            if (dx + dy < bestDistance) { bestDistance = dx + dy; best = input; }
        }
        return best;
    }
    function container(target, password) {
        if (!target || !password) return null;
        var form = target;
        var nearestForm = null;
        for (var depth = 0; depth <= 5 && form; depth++, form = form.parentElement) {
            if (form.tagName === 'FORM') { nearestForm = form; break; }
        }
        if (nearestForm && nearestForm.contains(password)) return nearestForm;
        for (var level = 0; level < 6 && target; level++, target = target.parentElement) {
            if (target.contains(password)) return target;
        }
        return null;
    }
    function associated(target) { return container(target, nearestPassword(target)) !== null; }
    function focusedInput(target) {
        return !!target && target.tagName === 'INPUT' && (target.type === 'password' || associated(target));
    }
    function credentials(target) {
        var password = nearestPassword(target);
        var root = container(target, password);
        if (!root) return {username: '', password: ''};
        var inputs = root.getElementsByTagName('input');
        var username = '';
        var value = '';
        var hints = ['user', 'name', 'username', '账号', '用户名', '邮箱', '手机号码', '手机号', 'email', 'login'];
        for (var index = 0; index < inputs.length; index++) {
            var input = inputs[index];
            if (username || (input.type !== 'text' && input.type !== 'email' && input.type !== 'number' && input.type !== 'tel')) {
                if (!value && input === password) value = input.value;
            } else {
                var placeholder = (input.placeholder || '').toLowerCase();
                for (var hint = 0; hint < hints.length; hint++) {
                    if (placeholder.indexOf(hints[hint]) !== -1) { username = input.value; break; }
                }
                if (!username) username = input.value;
            }
            if (username && value) break;
        }
        return {username: username, password: value};
    }
    function capture(target) {
        if (!target || throttled()) return;
        lastCapture = Date.now();
        try {
            var values = credentials(target);
            send(110, {user: values.username, pass: values.password, url: window.location.href});
            send(109, {show: 0});
        } catch (ignored) {}
    }
    function lower(text) { return text ? text.toLowerCase() : null; }
    function containsAny(text, words) {
        if (!text || !words) return false;
        for (var index = 0; index < words.length; index++) if (text.indexOf(words[index]) !== -1) return true;
        return false;
    }
    function clickedLogin(target) {
        for (var depth = 0; depth < 5 && target; depth++, target = target.parentElement) {
            var tag = target.tagName;
            var type = target.getAttribute('type');
            var button = tag === 'BUTTON' || tag === 'INPUT' && (type === 'button' || type === 'submit') ||
                tag === 'DIV' && target.getAttribute('role') === 'button' || tag === 'A' && !target.getAttribute('href');
            if (button && (lower(type) === 'submit' || containsAny(lower(target.className), loginClasses) ||
                containsAny(lower(target.id), loginClasses) ||
                containsAny(lower((target.innerText || target.value).replace(/\s/g, '')), loginWords))) {
                return associated(target) ? target : null;
            }
        }
        return null;
    }
    function start() {
        document.addEventListener('focusin', function (event) {
            if (focusedInput(event.target)) visibility(true, false);
        });
        document.addEventListener('focusout', function (event) { if (focusedInput(event.target)) visibility(false, false); });
        document.addEventListener('submit', function (event) { if (!throttled() && associated(event.target)) capture(event.target); }, true);
        document.addEventListener('click', function (event) {
            if (!throttled()) { var target = clickedLogin(event.target); if (target) capture(target); }
        }, true);
        window.addEventListener('beforeunload', function () { visibility(false, true); });
    }
    start();
})();

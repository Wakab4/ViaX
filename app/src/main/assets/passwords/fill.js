(function (credentials) {
    function visible(element) {
        return !(element.style && element.style.display === 'none' || element.hasAttribute('hidden') ||
            element.hasAttribute('aria-hidden') && element.getAttribute('aria-hidden') === 'true');
    }
    function fields(root) {
        var inputs = root.getElementsByTagName('input');
        var username = null;
        var password = null;
        var hints = ['user', 'name', 'username', '账号', '用户名', '邮箱', '手机号码', '手机号', 'email', 'login'];
        for (var index = 0; index < inputs.length; index++) {
            var input = inputs[index];
            if (!visible(input)) continue;
            if (username || input.type !== 'text' && input.type !== 'email' && input.type !== 'number') {
                if (!password && input.type === 'password') password = input;
            } else {
                var placeholder = (input.placeholder || '').toLowerCase();
                for (var hint = 0; hint < hints.length; hint++) {
                    if (placeholder.indexOf(hints[hint]) !== -1) { username = input; break; }
                }
                if (!username) username = input;
            }
            if (username && password) break;
        }
        return {username: username, password: password};
    }
    function setValue(element, text) {
        if (!element || typeof text !== 'string') return;
        var prototype = Object.getPrototypeOf(element);
        var property = prototype && Object.getOwnPropertyDescriptor(prototype, 'value');
        var setter = property && property.set;
        if (setter) setter.call(element, text); else element.value = text;
        element.dispatchEvent(new Event('input', {bubbles: true}));
        element.dispatchEvent(new Event('change', {bubbles: true}));
    }
    function container() {
        var active = document.activeElement;
        if (!active || active.tagName === 'BODY') {
            var inputs = document.getElementsByTagName('input');
            if (inputs.length === 0) return null;
            for (var index = 0; index < inputs.length; index++) {
                if (inputs[index].type === 'password') { active = inputs[index]; break; }
            }
            if (!active) return null;
        }
        var form = active.closest('form');
        if (form) return form;
        var nearest = active.closest('div');
        if (nearest) {
            for (var remaining = 6, current = nearest; current && remaining > 0; remaining--) {
                var found = fields(current);
                if (found.username && found.password) return current;
                current = current.parentElement.closest('div');
            }
            return nearest;
        }
        return null;
    }
    var root = container();
    if (!root) return 0;
    var found = fields(root);
    setValue(found.username, credentials.user);
    setValue(found.password, credentials.pass);
    return 1;
})(__VIA_PASSWORD_ARGUMENTS__);

(function () {
  if (typeof window.__setMarkerEnabled !== 'undefined') {
    return;
  }
  const secret = {{token}};
  let enabled = false, lastEvent = null, confirming = false, elements = [], position = -1;
  function send(action, extra) {
    window.via.postMessage(secret, JSON.stringify(Object.assign({action: action}, extra)));
  }
  let style = document.getElementById('__via_marker__');
  if (!style) {
    style = document.createElement('style'); style.type = 'text/css'; style.charset = 'UTF-8';
    style.id = '__via_marker__'; style.appendChild(document.createTextNode('')); document.head.appendChild(style);
  }
  function highlight(selector) {
    const currentStyle = document.getElementById('__via_marker__');
    if (currentStyle) currentStyle.innerText = selector ? selector + '{border:5px solid red!important;}' : '';
  }
  function current() { return elements[position] || null; }
  function rareClass(value) {
    if (!value) return null;
    let chosen = null, count = Infinity;
    for (const name of value.split(' ')) {
      const matches = name ? document.getElementsByClassName(name).length : 0;
      if (matches > 0 && matches < count) { count = matches; chosen = name; }
    }
    return chosen;
  }
  function attributeSelector(element) {
    let chosen = null, count = -1;
    for (const attribute of element.attributes) {
      if (!attribute || !attribute.value || !attribute.name) continue;
      const query = attribute.value.indexOf('?');
      const value = (query > 5 ? attribute.value.slice(0, query) : attribute.value).replaceAll('"', "'");
      const selector = element.localName + '[' + attribute.name + (query > 5 ? '^=' : '=') + '"' + value + '"]';
      const matches = document.querySelectorAll(selector).length;
      if (matches > 0 && (count < 0 || matches < count)) { count = matches; chosen = selector; }
    }
    return chosen;
  }
  function path(element, parts) {
    if (!element || parts.length >= 3) return parts;
    if (element.nodeName === 'BODY') { parts.push('body'); return parts; }
    if (typeof element !== 'object' || !element.getAttribute) return path(element.parentElement, parts);
    const id = element.getAttribute('id');
    if (id && document.getElementById(id) === element) { parts.push('#' + id); return parts; }
    const name = rareClass(element.getAttribute('class'));
    if (name) { parts.push('.' + name); return parts; }
    const attribute = attributeSelector(element);
    if (attribute) { parts.push(attribute); return parts; }
    parts.push(element.localName); return path(element.parentElement, parts);
  }
  function select(element) {
    if (!element) return;
    const found = elements.indexOf(element);
    if (found < 0) { elements = [element]; position = 0; } else position = found;
    const selector = path(element, []).reverse().join('>');
    highlight(selector);
    if (confirming) {
      try { if (location.host) send(103, {host: location.host, filter: selector}); } catch (ignored) {}
      confirming = false;
    } else send(102, {filter: selector});
  }
  function handle(event) {
    lastEvent = event;
    if (enabled) { select(event.srcElement || event.target || event); event.preventDefault(); }
  }
  window.__setMarkerEnabled = function (value) {
    enabled = value;
    if (value === false) highlight(null); else if (lastEvent) select(lastEvent.srcElement || lastEvent.target || lastEvent);
  };
  window.__getMarkerFilter = function () { const element = current(); if (element) { confirming = true; select(element); } };
  window.__markParent = function () {
    if (position > 0) select(elements[position - 1]);
    else { const element = current(); if (element && element.parentNode) { elements.splice(0, 0, element.parentNode); select(element.parentNode); } }
  };
  window.__markChild = function () {
    if (position < elements.length - 1) select(elements[position + 1]);
    else { const element = current(); if (element && element.firstElementChild) { elements.splice(position + 1, 0, element.firstElementChild); select(element.firstElementChild); } }
  };
  for (const name of ['touchstart', 'touchend', 'touchmove', 'touchcancel', 'touchdown', 'click']) {
    document.removeEventListener(name, handle); document.addEventListener(name, handle, false);
  }
})();

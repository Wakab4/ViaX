(function () {
  var marker = "via-fake-print";
  if (!window[marker]) try {
    window[marker] = true;
    var original = window.print;
    if (typeof original === "function" && /\{\s*\[native code\]\s*\}/.test(Function.prototype.toString.call(original))) {
      window.print = function () {
        window.via.cmd(516);
      };
    }
  } catch (error) {}
})();

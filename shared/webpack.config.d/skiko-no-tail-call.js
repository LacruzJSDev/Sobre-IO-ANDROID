// Safari 27.2 (macOS and iOS) paints the web app as a white page: WebKit bug 329421 (Compose CMP-10986). Skiko's
// JS wrappers tail-call into its wasm module, and once WebKit's JIT optimises the caller the float arguments arrive as
// NaN, so every drawing gets absurd sizes. Each wrapper of skiko.mjs is rewritten so the call sits inside a try block,
// where it is not a tail call. The build fails if no wrapper is found (skiko changed its shape: check this file) or if
// a tail-calling wrapper survives in the minified bundle.
// Remove this file once the Safari versions in use carry the WebKit fix (it is in Safari Technology Preview 255).
const fs = require('fs');
const path = require('path');

// webpack loads loaders from files, so the loader is written next to the generated webpack.config.js
const loaderPath = path.join(__dirname, 'skiko-no-tail-call-loader.cjs');
fs.writeFileSync(loaderPath, `
module.exports = function (source) {
  const wrapper = /\\(\\.\\.\\.a\\) => \\(([A-Za-z0-9_$]+) = loadedWasm\\._\\["\\1"\\]\\)\\(\\.\\.\\.a\\)/g;
  let count = 0;
  const rewritten = source.replace(wrapper, (match, name) => {
    count++;
    return '(...a) => { try { return (' + name + ' = loadedWasm._["' + name + '"])(...a); } catch (e) { throw e; } }';
  });
  if (count === 0) {
    throw new Error('skiko-no-tail-call: no skiko wrapper found in skiko.mjs; its shape changed (see webpack.config.d/skiko-no-tail-call.js)');
  }
  return rewritten;
};
`);

config.module = config.module || {};
config.module.rules = config.module.rules || [];
config.module.rules.push({ test: /[\\/]skiko\.mjs$/, use: [loaderPath] });

// After minification no wrapper may tail-call any more: `(...e)=>(x=y._.z)(...e)` is the minified form of the original
config.plugins = config.plugins || [];
config.plugins.push({
  apply(compiler) {
    compiler.hooks.thisCompilation.tap('SkikoNoTailCallCheck', (compilation) => {
      const { Compilation, WebpackError } = compiler.webpack;
      compilation.hooks.processAssets.tap(
        { name: 'SkikoNoTailCallCheck', stage: Compilation.PROCESS_ASSETS_STAGE_ANALYSE },
        (assets) => {
          const tailCall = /=>\(([A-Za-z0-9_$]+)=[A-Za-z0-9_$]+\._(?:\.[A-Za-z0-9_$]+|\["[^"]+"\])\)\(\.\.\.[A-Za-z0-9_$]+\)/;
          for (const name of Object.keys(assets)) {
            if (!/\.m?js$/.test(name)) continue;
            const found = assets[name].source().toString().match(tailCall);
            if (found) {
              compilation.errors.push(new WebpackError('skiko-no-tail-call: ' + name + ' still tail-calls skiko: ' + found[0]));
            }
          }
        },
      );
    });
  },
});

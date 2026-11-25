const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const out = path.join(root, 'www');

const FILES = [
  'index.html',
  'styles.css',
  'manifest.webmanifest',
  'sw.js'
];

const DIRS = [
  'assets',
  'data',
  'icons',
  'js'
];

function copyRecursive(src, dest) {
  const stat = fs.statSync(src);
  if (stat.isDirectory()) {
    fs.mkdirSync(dest, { recursive: true });
    for (const entry of fs.readdirSync(src)) {
      copyRecursive(path.join(src, entry), path.join(dest, entry));
    }
  } else {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
  }
}

fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out, { recursive: true });

for (const file of FILES) {
  const src = path.join(root, file);
  if (fs.existsSync(src)) {
    fs.copyFileSync(src, path.join(out, file));
  }
}

for (const dir of DIRS) {
  const src = path.join(root, dir);
  if (fs.existsSync(src)) {
    copyRecursive(src, path.join(out, dir));
  }
}

console.log('Built www/ cleanly for Capacitor.');

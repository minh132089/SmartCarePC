import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import ts from 'typescript'

const vietnamese = /[ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝàáâãèéêìíòóôõùúýĂăĐđĨĩŨũƠơƯư\u1ea0-\u1ef9]|\b(?:sao|thanh)\b/u
const failures = []
function check(file) {
  const code = fs.readFileSync(file, 'utf8')
  if (file.endsWith('.json')) {
    function visit(value) {
      if (typeof value === 'string' && vietnamese.test(value)) failures.push(`${file}: ${value}`)
      else if (value && typeof value === 'object') Object.values(value).forEach(visit)
    }
    visit(JSON.parse(code))
    return
  }
  const source = ts.createSourceFile(file, code, ts.ScriptTarget.Latest, true)
  function visit(node) {
    const literal = ts.isStringLiteralLike(node) || ts.isTemplateHead(node) || ts.isTemplateMiddle(node) || ts.isTemplateTail(node) || ts.isJsxText(node)
    // Old occupation preset keys translate existing profiles without rewriting research data.
    const legacyKey = node.parent && ts.isPropertyAssignment(node.parent) && node.parent.name === node
    if (literal && !legacyKey && vietnamese.test(node.text)) {
      failures.push(`${file}:${source.getLineAndCharacterOfPosition(node.getStart(source)).line + 1}: ${node.text}`)
    }
    ts.forEachChild(node, visit)
  }
  visit(source)
}
function walk(folder) {
  for (const entry of fs.readdirSync(folder, { withFileTypes: true })) {
    const file = path.join(folder, entry.name)
    if (entry.isDirectory()) walk(file)
    else if (/\.(?:tsx?|json)$/.test(file)) check(file)
  }
}
walk('src')
check('functions/api/[[path]].js')
assert.match(fs.readFileSync('index.html', 'utf8'), /<html lang="en">/)
assert.deepEqual(failures, [], 'User-facing source copy must be in English')
console.log('PASS: English UI copy, accessibility labels, data labels and proxy errors.')

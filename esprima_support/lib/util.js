'use strict';

const esprima = require('esprima');
const Syntax = esprima.Syntax;

/**
 * test if an array consists of static literals
 * @param  {Node} node AST node
 * @return {Boolean}      is static
 */
const isStatic = exports.isStatic = function(node) {
  if (!node) 
    return false;
  
  switch (node.type) {
    case Syntax.Literal:
      return true;

    case Syntax.ArrayExpression:
      return node.elements.every(isStatic);

    case Syntax.ObjectExpression:
      return node.properties.every(
        property => isStatic(property.value) && [Syntax.Literal, Syntax.Identifier]
        .indexOf(property.key.type) > -1);

    default:
      return false;
  }
};

/**
 * parse an expression
 * @param  {Node} node AST node
 * @return {Any}      actual value
 */
const parseStatic = exports.parseStatic = function(node) {
  if (!node) 
    return false;

  switch (node.type) {
    case Syntax.Literal:
      return node.value;

    case Syntax.ArrayExpression:
      return node.elements.map(parseStatic);

    case Syntax.ObjectExpression:
      let obj = {};
      node.properties.forEach(property => obj[property.key.name ||
        property.key.value] = parseStatic(property.value));
      return obj;

    default:
      // Unknown expression
      return null;
  }
};

/**
 * test if call arguments are constant
 * @param  {Node} node AST node
 * @return {Boolean}      is static
 */
exports.isStaticArguments = node =>
  node.type === Syntax.CallExpression &&
  node.arguments.every(isStatic);

/**
 * parse arguments list to an array
 * @param  {Node} node AST node
 * @return {Array}      Actual value of arguments
 */
exports.parseArguments = node => 
  node.arguments.map(parseStatic);

/**
 * convert code to AST
 * @param  {[type]} code [description]
 * @return {[type]}      [description]
 */
exports.expression = code => 
  esprima.parse(code).body[0].expression;

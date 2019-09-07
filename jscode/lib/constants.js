'use strict';

const Constants = {
  Properties: {
    'string': ['length'],
    'regex': ['source'],
    'array': ['length']
  },
  Methods: {
    'string': ['anchor', 'big', 'blink', 'bold', 'charAt', 'charCodeAt',
      'codePointAt', 'concat', 'endsWith', 'fixed', 'fontcolor',
      'fontsize', 'includes', 'indexOf', 'italics', 'lastIndexOf',
      'length', 'link', 'localeCompare', 'match', 'normalize', 'repeat',
      'replace', 'search', 'slice', 'small', 'split', 'startsWith',
      'strike', 'sub', 'substr', 'substring', 'sup', 'toLocaleLowerCase',
      'toLocaleUpperCase', 'toLowerCase', 'toString', 'toUpperCase',
      'trim', 'trimLeft', 'trimRight'
    ],
    'number': ['toString'],
    'array': ['concat', 'indexOf', 'join', 'lastIndexOf', 'pop',
      'push', 'reverse', 'shift', 'slice', 'unshift'
    ]
  },
  Objects: {
    'String': ['apply', 'call', 'fromCharCode', 'fromCodePoint'],
    'Date': ['parse', 'UTC'],
    'Number': ['parseInt', 'parseFloat', 'isNaN', 'isFinite',
      'isInteger', 'isSafeInteger'
    ],
    'Math': ['abs', 'acos', 'acosh', 'asin', 'asinh', 'atan', 'atan2',
      'atanh', 'cbrt', 'ceil', 'clz32', 'cos', 'cosh', 'exp', 'expm1',
      'floor', 'fround', 'hypot', 'imul', 'log', 'log10', 'log1p',
      'log2', 'max', 'min', 'pow', 'round', 'sign', 'sin', 'sinh',
      'sqrt', 'tan', 'tanh', 'trunc'
    ],
    'global': ['Infinity', 'NaN']
  },
  Constants: {
    'Number': ['EPSILON', 'MAX_SAFE_INTEGER', 'MAX_VALUE',
      'MIN_SAFE_INTEGER', 'MIN_VALUE', 'NaN', 'NEGATIVE_INFINITY',
      'POSITIVE_INFINITY'
    ],
    'Math': ['E', 'LN10', 'LN2', 'LOG10E', 'LOG2E', 'PI', 'SQRT1_2', 'SQRT2'],
  },
  Functions: ['isFinite', 'isNaN', 'parseFloat', 'parseInt', 'decodeURI',
    'decodeURIComponent', 'encodeURI', 'encodeURIComponent', 'escape',
    'atob', 'btoa'
  ]
};

module.exports = Constants;
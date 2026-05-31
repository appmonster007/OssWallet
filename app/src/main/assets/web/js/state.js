/* Shared mutable state — loaded first, referenced by all other modules */
const baseUrl = window.location.origin;
let token            = sessionStorage.getItem('oss_token') || '';
let cachedCategories = [];
let cachedAccounts   = [];
let cachedSettings   = { baseCurrency: 'USD', decimalPrecision: 2 };
let cachedTags       = [];
let cardOverrides    = {};

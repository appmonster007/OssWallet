/* Mock fixture data — only active when ?mock=true */
/* Keys match API paths; values are the response body returned by the server. */
if (typeof window.MOCK_DATA === 'undefined') {
window.MOCK_DATA = {};

/* ────────────── SETTINGS ────────────── */
MOCK_DATA['/api/settings'] = {
  success: true,
  data: {
    profileName: 'Alex Morgan',
    baseCurrency: 'USD',
    decimalPrecision: 2,
    theme: 'system',
    language: 'en',
    firstDayOfWeek: 'MONDAY',
    dateFormat: 'MMM d, yyyy',
    enableBiometric: false,
    enableWebServer: true,
  }
};

/* ────────────── ACCOUNTS ────────────── */
const MOCK_ACCOUNTS = [
  { id: 'acc-1', name: 'Main Checking',    currency: 'USD', type: 'CHECKING',    balance: 4830.25,   colorHex: '#42A5F5', income: 5200.00,  expense: -3750.00, transfer: -200.00 },
  { id: 'acc-2', name: 'Savings',           currency: 'USD', type: 'SAVINGS',    balance: 12400.00,  colorHex: '#66BB6A', income: 1200.00,  expense: 0,        transfer: 200.00  },
  { id: 'acc-3', name: 'Travel Visa',       currency: 'USD', type: 'CREDIT_CARD',balance: -1245.80,  colorHex: '#EC407A', income: 0,        expense: -1245.80, transfer: 0,  creditLimit: 5000 },
  { id: 'acc-4', name: 'EUR Wallet',        currency: 'EUR', type: 'CHECKING',   balance: 870.00,    colorHex: '#FFA726', income: 870.00,   expense: -120.00,  transfer: 0       },
  { id: 'acc-5', name: 'Investment Port',   currency: 'USD', type: 'INVESTMENT', balance: 28900.00,  colorHex: '#7E57C2', income: 3100.00,  expense: 0,        transfer: -500.00 },
  { id: 'acc-6', name: 'Cash on Hand',      currency: 'USD', type: 'CASH',       balance: 180.00,    colorHex: '#26C6DA', income: 300.00,   expense: -220.00,  transfer: 0       },
];

MOCK_DATA['/api/accounts'] = { success: true, data: MOCK_ACCOUNTS };

MOCK_DATA['/api/accounts/acc-1'] = {
  success: true,
  data: {
    account: MOCK_ACCOUNTS[0],
    recentTransactions: [
      { id: 101, title: 'Whole Foods',      amount: -127.45, currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*0,  categoryName: 'Groceries',    accountName: 'Main Checking' },
      { id: 102, title: 'Salary — July',    amount: 5200.00, currency: 'USD', type: 'INCOME',   date: Date.now() - 86400000*1,  categoryName: 'Salary',       accountName: 'Main Checking' },
      { id: 103, title: 'Netflix',          amount: -15.99,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*2,  categoryName: 'Entertainment',accountName: 'Main Checking' },
      { id: 104, title: 'Electric Bill',    amount: -94.20,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*3,  categoryName: 'Utilities',    accountName: 'Main Checking' },
      { id: 105, title: 'Transfer to Savings', amount: -200.00,currency: 'USD',type: 'TRANSFER',date: Date.now() - 86400000*4,  categoryName: null,           accountName: 'Main Checking' },
      { id: 106, title: 'Uber',             amount: -23.50,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*5,  categoryName: 'Transport',    accountName: 'Main Checking' },
      { id: 107, title: 'Amazon order',     amount: -89.99,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*6,  categoryName: 'Shopping',     accountName: 'Main Checking' },
      { id: 108, title: 'Coffee & Biscuits',amount: -8.40,   currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*7,  categoryName: 'Food & Drink', accountName: 'Main Checking' },
    ]
  }
};

MOCK_DATA['/api/accounts/acc-2'] = {
  success: true,
  data: { account: MOCK_ACCOUNTS[1], recentTransactions: [
    { id: 201, title: 'Transfer from Checking', amount: 200.00, currency: 'USD', type: 'TRANSFER', date: Date.now() - 86400000*4, categoryName: null, accountName: 'Savings' },
    { id: 202, title: 'Interest',                amount: 12.40, currency: 'USD', type: 'INCOME',   date: Date.now() - 86400000*30, categoryName: 'Interest', accountName: 'Savings' },
  ]}
};

MOCK_DATA['/api/accounts/acc-3'] = {
  success: true,
  data: { account: MOCK_ACCOUNTS[2], recentTransactions: [
    { id: 301, title: 'Hotel Barcelona',  amount: -340.00, currency: 'USD', type: 'EXPENSE', date: Date.now() - 86400000*2,  categoryName: 'Travel', accountName: 'Travel Visa' },
    { id: 302, title: 'Flight EZY4561',   amount: -189.00, currency: 'USD', type: 'EXPENSE', date: Date.now() - 86400000*8,  categoryName: 'Travel', accountName: 'Travel Visa' },
    { id: 303, title: 'Airport Lounge',   amount: -45.00,  currency: 'USD', type: 'EXPENSE', date: Date.now() - 86400000*8,  categoryName: 'Travel', accountName: 'Travel Visa' },
    { id: 304, title: 'Dinner — El Born', amount: -67.80,  currency: 'USD', type: 'EXPENSE', date: Date.now() - 86400000*3,  categoryName: 'Food & Drink', accountName: 'Travel Visa' },
  ]}
};

/* ────────────── CATEGORIES ────────────── */
const MOCK_CATEGORIES = [
  { id: 1,  name: 'Groceries',      colorHex: '#66BB6A', type: 'EXPENSE' },
  { id: 2,  name: 'Salary',         colorHex: '#42A5F5', type: 'INCOME'  },
  { id: 3,  name: 'Entertainment',  colorHex: '#EC407A', type: 'EXPENSE' },
  { id: 4,  name: 'Utilities',      colorHex: '#FFA726', type: 'EXPENSE' },
  { id: 5,  name: 'Transport',      colorHex: '#26C6DA', type: 'EXPENSE' },
  { id: 6,  name: 'Shopping',       colorHex: '#7E57C2', type: 'EXPENSE' },
  { id: 7,  name: 'Food & Drink',   colorHex: '#EF5350', type: 'EXPENSE' },
  { id: 8,  name: 'Health',         colorHex: '#26A69A', type: 'EXPENSE' },
  { id: 9,  name: 'Travel',         colorHex: '#FF7043', type: 'EXPENSE' },
  { id: 10, name: 'Rent',           colorHex: '#8D6E63', type: 'EXPENSE' },
  { id: 11, name: 'Freelance',      colorHex: '#4DCE85', type: 'INCOME'  },
  { id: 12, name: 'Interest',       colorHex: '#FFEE58', type: 'INCOME'  },
  { id: 13, name: 'Investments',    colorHex: '#7E57C2', type: 'INCOME'  },
  { id: 14, name: 'Subscriptions',  colorHex: '#78909C', type: 'EXPENSE' },
  { id: 15, name: 'Pets',           colorHex: '#A5D6A7', type: 'EXPENSE' },
];

MOCK_DATA['/api/categories'] = { success: true, data: MOCK_CATEGORIES };

/* ────────────── TAGS ────────────── */
MOCK_DATA['/api/tags'] = {
  success: true,
  data: [
    { name: 'work' }, { name: 'personal' }, { name: 'recurring' },
    { name: 'travel' }, { name: 'family' }, { name: 'health' },
    { name: 'tax-deductible' }, { name: 'reimbursable' },
  ]
};

/* ────────────── TRANSACTIONS ────────────── */
const MOCK_TRANSACTIONS = [
  { id: 101, title: 'Whole Foods',         amount: 127.45, currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*0,  categoryName: 'Groceries',     categoryId: 1,  accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'CARD',  tags: ['personal'], note: '' },
  { id: 102, title: 'Salary — July',       amount: 5200.00,currency: 'USD', type: 'INCOME',   date: Date.now() - 86400000*1,  categoryName: 'Salary',        categoryId: 2,  accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'BANK_TRANSFER', tags: ['work', 'recurring'], note: 'Monthly salary' },
  { id: 103, title: 'Netflix',             amount: 15.99,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*2,  categoryName: 'Entertainment', categoryId: 3,  accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'CARD',  tags: ['recurring'], note: '' },
  { id: 104, title: 'Electric Bill',       amount: 94.20,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*3,  categoryName: 'Utilities',     categoryId: 4,  accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'BANK_TRANSFER', tags: ['recurring'], note: '' },
  { id: 105, title: 'Transfer to Savings', amount: 200.00, currency: 'USD', type: 'TRANSFER', date: Date.now() - 86400000*4,  categoryName: null,            categoryId: null,accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'BANK_TRANSFER', tags: [], note: '' },
  { id: 106, title: 'Uber',               amount: 23.50,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*5,  categoryName: 'Transport',     categoryId: 5,  accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'DIGITAL_WALLET', tags: [], note: '' },
  { id: 107, title: 'Amazon order',       amount: 89.99,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*6,  categoryName: 'Shopping',      categoryId: 6,  accountName: 'Main Checking', accountId: 'acc-1', status: 'UNCLEARED', paymentMethod: 'CARD',  tags: ['reimbursable'], note: 'Office supplies' },
  { id: 108, title: 'Coffee & Biscuits',  amount: 8.40,   currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*7,  categoryName: 'Food & Drink',  categoryId: 7,  accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'CASH',  tags: [], note: '' },
  { id: 109, title: 'Doctor Visit',       amount: 45.00,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*8,  categoryName: 'Health',        categoryId: 8,  accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'CARD',  tags: ['health'], note: '' },
  { id: 110, title: 'Freelance Project',  amount: 800.00, currency: 'USD', type: 'INCOME',   date: Date.now() - 86400000*9,  categoryName: 'Freelance',     categoryId: 11, accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'BANK_TRANSFER', tags: ['work', 'tax-deductible'], note: 'Design work for Acme' },
  { id: 111, title: 'Hotel Barcelona',   amount: 340.00, currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*10, categoryName: 'Travel',        categoryId: 9,  accountName: 'Travel Visa',   accountId: 'acc-3', status: 'CLEARED',   paymentMethod: 'CARD',  tags: ['travel'], note: '' },
  { id: 112, title: 'Flight EZY4561',    amount: 189.00, currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*11, categoryName: 'Travel',        categoryId: 9,  accountName: 'Travel Visa',   accountId: 'acc-3', status: 'CLEARED',   paymentMethod: 'CARD',  tags: ['travel'], note: '' },
  { id: 113, title: 'Spotify',           amount: 9.99,   currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*12, categoryName: 'Subscriptions', categoryId: 14, accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'CARD',  tags: ['recurring'], note: '' },
  { id: 114, title: 'Rent — July',       amount: 1800.00,currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*13, categoryName: 'Rent',          categoryId: 10, accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'BANK_TRANSFER', tags: ['recurring'], note: '' },
  { id: 115, title: 'Dog grooming',      amount: 60.00,  currency: 'USD', type: 'EXPENSE',  date: Date.now() - 86400000*14, categoryName: 'Pets',          categoryId: 15, accountName: 'Main Checking', accountId: 'acc-1', status: 'CLEARED',   paymentMethod: 'CASH',  tags: ['family'], note: '' },
];

MOCK_DATA['/api/transactions'] = {
  success: true,
  data: { items: MOCK_TRANSACTIONS, total: 15, page: 0, limit: 25, hasMore: false }
};

MOCK_DATA['/api/transactions/101'] = { success: true, data: MOCK_TRANSACTIONS[0] };
MOCK_DATA['/api/transactions/102'] = { success: true, data: MOCK_TRANSACTIONS[1] };

/* ────────────── BUDGETS ────────────── */
const MOCK_BUDGETS = [
  { id: 'b-1', name: 'Groceries',     currency: 'USD', limitAmount: 400.00, spent: 127.45, remaining: 272.55, period: 'Monthly', isArchived: false, categoryIds: [1] },
  { id: 'b-2', name: 'Entertainment', currency: 'USD', limitAmount: 100.00, spent: 25.98,  remaining: 74.02,  period: 'Monthly', isArchived: false, categoryIds: [3,14] },
  { id: 'b-3', name: 'Transport',     currency: 'USD', limitAmount: 150.00, spent: 138.50, remaining: 11.50,  period: 'Monthly', isArchived: false, categoryIds: [5] },
  { id: 'b-4', name: 'Dining Out',    currency: 'USD', limitAmount: 200.00, spent: 210.40, remaining: -10.40, period: 'Monthly', isArchived: false, categoryIds: [7] },
  { id: 'b-5', name: 'Travel 2024',   currency: 'USD', limitAmount: 3000.00,spent: 594.00, remaining: 2406.00,period: 'Yearly',  isArchived: false, categoryIds: [9] },
];

MOCK_DATA['/api/budgets'] = { success: true, data: MOCK_BUDGETS };
MOCK_DATA['/api/budgets/b-1'] = { success: true, data: MOCK_BUDGETS[0] };
MOCK_DATA['/api/budgets/b-2'] = { success: true, data: MOCK_BUDGETS[1] };

/* ────────────── LOANS ────────────── */
const MOCK_LOANS = [
  { id: 'l-1', name: 'Car Loan',      loanType: 'BORROWED', lenderOrBorrowerName: 'First Bank',  currency: 'USD', principalAmount: 18000.00, totalPaid: 6300.00, remainingBalance: 11700.00, status: 'ACTIVE',  startDate: Date.now() - 86400000*540, expectedSettlementDate: Date.now() + 86400000*360, interestAccrued: 420.00 },
  { id: 'l-2', name: 'Loan to Jake',  loanType: 'LENT',     lenderOrBorrowerName: 'Jake Carter', currency: 'USD', principalAmount: 500.00,   totalPaid: 250.00,  remainingBalance: 250.00,   status: 'ACTIVE',  startDate: Date.now() - 86400000*60,  expectedSettlementDate: Date.now() + 86400000*60,  interestAccrued: 0 },
  { id: 'l-3', name: 'Personal Loan', loanType: 'BORROWED', lenderOrBorrowerName: 'Friend Sam',  currency: 'USD', principalAmount: 1200.00,  totalPaid: 1200.00, remainingBalance: 0,        status: 'SETTLED', startDate: Date.now() - 86400000*120, expectedSettlementDate: null, interestAccrued: 0 },
];

MOCK_DATA['/api/loans'] = { success: true, data: MOCK_LOANS };
MOCK_DATA['/api/loans/l-1'] = { success: true, data: MOCK_LOANS[0] };
MOCK_DATA['/api/loans/l-1/payments'] = { success: true, data: [
  { id: 'lp-1', amount: 350.00, currency: 'USD', date: Date.now() - 86400000*30, note: 'Monthly payment' },
  { id: 'lp-2', amount: 350.00, currency: 'USD', date: Date.now() - 86400000*60, note: 'Monthly payment' },
] };

/* ────────────── PLANNED PAYMENTS ────────────── */
const NOW = Date.now();
const DAY = 86400000;
const MOCK_PLANNED = [
  { id: 'pp-1', title: 'Rent',          amount: 1800.00, currency: 'USD', transactionType: 'EXPENSE', recurrenceType: 'MONTHLY',   nextOccurrenceDate: NOW + DAY*3,  categoryName: 'Rent',          accountId: 'acc-1', accountName: 'Main Checking' },
  { id: 'pp-2', title: 'Netflix',       amount: 15.99,   currency: 'USD', transactionType: 'EXPENSE', recurrenceType: 'MONTHLY',   nextOccurrenceDate: NOW + DAY*7,  categoryName: 'Entertainment', accountId: 'acc-1', accountName: 'Main Checking' },
  { id: 'pp-3', title: 'Salary',        amount: 5200.00, currency: 'USD', transactionType: 'INCOME',  recurrenceType: 'MONTHLY',   nextOccurrenceDate: NOW + DAY*12, categoryName: 'Salary',        accountId: 'acc-1', accountName: 'Main Checking' },
  { id: 'pp-4', title: 'Gym Membership',amount: 45.00,   currency: 'USD', transactionType: 'EXPENSE', recurrenceType: 'MONTHLY',   nextOccurrenceDate: NOW - DAY*2,  categoryName: 'Health',        accountId: 'acc-1', accountName: 'Main Checking' },
  { id: 'pp-5', title: 'Internet Bill', amount: 79.99,   currency: 'USD', transactionType: 'EXPENSE', recurrenceType: 'MONTHLY',   nextOccurrenceDate: NOW - DAY*5,  categoryName: 'Utilities',     accountId: 'acc-1', accountName: 'Main Checking' },
  { id: 'pp-6', title: 'Car Insurance', amount: 142.00,  currency: 'USD', transactionType: 'EXPENSE', recurrenceType: 'MONTHLY',   nextOccurrenceDate: NOW + DAY*18, categoryName: 'Transport',     accountId: 'acc-1', accountName: 'Main Checking' },
];

MOCK_DATA['/api/planned-payments']            = { success: true, data: MOCK_PLANNED };
MOCK_DATA['/api/planned-payments/upcoming']   = { success: true, data: MOCK_PLANNED.filter(p => p.nextOccurrenceDate > NOW) };
MOCK_DATA['/api/planned-payments/overdue']    = { success: true, data: MOCK_PLANNED.filter(p => p.nextOccurrenceDate < NOW) };
MOCK_DATA['/api/planned-payments/pp-1']       = { success: true, data: MOCK_PLANNED[0] };

/* ────────────── TEMPLATES ────────────── */
MOCK_DATA['/api/templates'] = {
  success: true,
  data: [
    { id: 't-1', title: 'Grocery Run',  amount: 80.00,  currency: 'USD', type: 'EXPENSE', categoryName: 'Groceries', accountId: 'acc-1', accountName: 'Main Checking', tags: ['personal'] },
    { id: 't-2', title: 'Freelance Inv',amount: 800.00, currency: 'USD', type: 'INCOME',  categoryName: 'Freelance', accountId: 'acc-1', accountName: 'Main Checking', tags: ['work', 'tax-deductible'] },
    { id: 't-3', title: 'Coffee',       amount: 5.50,   currency: 'USD', type: 'EXPENSE', categoryName: 'Food & Drink', accountId: 'acc-1', accountName: 'Main Checking', tags: [] },
  ]
};

/* ────────────── SAVED FILTERS ────────────── */
MOCK_DATA['/api/filters'] = {
  success: true,
  data: [
    { id: 'f-1', name: 'This Month Expenses', timelineType: 'MONTH', transactionTypes: ['EXPENSE'], accountIds: [], categoryIds: [], tags: [], statuses: [], currencies: [] },
    { id: 'f-2', name: 'Recurring Bills',      timelineType: 'ALL_TIME', tags: ['recurring'],        accountIds: [], categoryIds: [], transactionTypes: [], statuses: [], currencies: [] },
    { id: 'f-3', name: 'Work Related',         timelineType: 'ALL_TIME', tags: ['work', 'tax-deductible'], accountIds: [], categoryIds: [], transactionTypes: [], statuses: [], currencies: [] },
  ]
};

/* ────────────── DASHBOARD CARDS ────────────── */
const mkFilter = (tl = 'MONTH') => ({
  timelineType: tl, dateFrom: null, dateTo: null,
  accountIds: [], categoryIds: [], transactionTypes: [], tags: [],
  currencies: [], statuses: [], paymentMethods: [], queryNote: '',
  queryPayee: '', minAmount: null, maxAmount: null, excludeFuture: true,
});

const LABELS_6M = ['Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'];
const mkBucket  = (label, income, expense) => ({ label, income, expense, net: income + expense });

const DASH_CARDS = [
  {
    cardId: 'dc-accounts', cardType: 'ACCOUNTS', title: 'My Accounts',
    order: 0, isPinned: true, isEnabled: true,
    filterSummary: mkFilter('ALL_TIME'), config: { layout: 'grid', gridColumns: 2, perspective: 'Accounting', showAddAccountCard: false },
    data: {
      type: 'Accounts', currency: 'USD', layout: 'grid', gridColumns: 2, perspective: 'Accounting', showAddAccountCard: true,
      accounts: MOCK_ACCOUNTS.map(a => ({ ...a, balanceInBase: a.balance })),
    }
  },
  {
    cardId: 'dc-networth', cardType: 'NET_WORTH', title: 'Net Worth',
    order: 1, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { perspective: 'Accounting', showComparison: true, fontSize: 'Medium' },
    data: {
      type: 'NetWorth', netWorth: 45164.45, income: 6000.00, expense: -3970.44, transfer: 0,
      previousNetWorth: 43200.00, previousIncome: 5800.00, previousExpense: -3500.00, previousTransfer: 0,
      currency: 'USD', fontSize: 'Medium', perspective: 'Accounting', showComparison: true,
    }
  },
  {
    cardId: 'dc-cashflow', cardType: 'CASH_FLOW', title: 'Cash Flow',
    order: 2, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { perspective: 'Accounting', showNetColumn: true, showBaseConversion: true },
    data: {
      type: 'CashFlow', currency: 'USD', perspective: 'Accounting', showNetColumn: true, showBaseConversion: true,
      income: 6000.00, expense: -3970.44, transfer: -200.00, net: 1829.56,
      breakdown: [
        { currency: 'USD', income: 6000, incomeInBase: 6000, expense: -3970.44, expenseInBase: -3970.44, transfer: -200, transferInBase: -200, transferIn: 0, transferOut: 200, transferInInBase: 0, transferOutInBase: 200, net: 1829.56 },
        { currency: 'EUR', income: 870,  incomeInBase: 948,  expense: -120,     expenseInBase: -131,     transfer: 0,     transferInBase: 0,    transferIn: 0, transferOut: 0,   transferInInBase: 0, transferOutInBase: 0, net: 750 },
      ]
    }
  },
  {
    cardId: 'dc-recent', cardType: 'RECENT_ACTIVITIES', title: 'Recent Activities',
    order: 3, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('WEEK'), config: { showCategory: true, showAccount: true, showNote: false, showTags: true },
    data: {
      type: 'RecentActivities', showCategory: true, showAccount: true, showNote: false, showTags: true,
      transactions: MOCK_TRANSACTIONS.slice(0, 8).map(tx => ({
        ...tx, isPlanned: false,
        amount: Math.abs(tx.amount),
      }))
    }
  },
  {
    cardId: 'dc-currency', cardType: 'CURRENCY_BALANCES', title: 'Currency Balances',
    order: 4, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('ALL_TIME'), config: { layout: 'grid', gridColumns: 2, showBaseConversion: true },
    data: {
      type: 'CurrencyBalances', baseCurrency: 'USD', layout: 'grid', gridColumns: 2, showBaseConversion: true,
      items: [
        { currency: 'USD', balance: 17410.25, balanceInBase: 17410.25 },
        { currency: 'EUR', balance: 870.00,   balanceInBase: 948.30   },
      ]
    }
  },
  {
    cardId: 'dc-budgets', cardType: 'BUDGETS', title: 'Budgets',
    order: 5, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: {},
    data: {
      type: 'Budgets', currency: 'USD',
      budgets: MOCK_BUDGETS.filter(b => !b.isArchived).map(b => ({
        id: b.id, name: b.name, currency: b.currency,
        spentAmount: b.spent, limitAmount: b.limitAmount,
        remainingAmount: b.remaining,
        spentPercentage: Math.round((b.spent / b.limitAmount) * 100),
      }))
    }
  },
  {
    cardId: 'dc-planned', cardType: 'PLANNED_PAYMENTS', title: 'Planned Payments',
    order: 6, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: {},
    data: {
      type: 'PlannedPayments', currency: 'USD',
      payments: MOCK_PLANNED.map(p => ({ ...p }))
    }
  },
  {
    cardId: 'dc-loans', cardType: 'LOANS', title: 'Loans',
    order: 7, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('ALL_TIME'), config: {},
    data: {
      type: 'Loans', currency: 'USD',
      loans: MOCK_LOANS.map(l => ({
        id: l.id, name: l.name, type: l.loanType,
        principal: l.principalAmount, outstanding: l.remainingBalance,
        currency: l.currency, paidAmount: l.totalPaid,
        paidPercentage: Math.round((l.totalPaid / l.principalAmount) * 100),
        loanType: l.loanType, status: l.status,
        lenderOrBorrowerName: l.lenderOrBorrowerName,
      }))
    }
  },
  {
    cardId: 'dc-hero', cardType: 'ACCOUNT_HERO', title: 'Account Hero',
    order: 8, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { perspective: 'Accounting', showCurrencyBadges: true },
    data: {
      type: 'AccountHero', currency: 'USD', colorHex: '#42A5F5',
      balance: 4830.25, balanceInBase: 4830.25,
      income: 5200.00, expense: -3750.00, transferIn: 0, transferOut: 200.00,
      perspective: 'Accounting', showCurrencyBadges: true,
      distribution: { USD: 4830.25, EUR: 870.00 },
    }
  },
  {
    cardId: 'dc-catpie', cardType: 'CATEGORY_PIE_CHART', title: 'Category Breakdown',
    order: 9, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { perspective: 'Accounting' },
    data: {
      type: 'CategoryBreakdown', displayCurrency: 'USD',
      items: [
        { categoryId: 10, categoryName: 'Rent',          amount: -1800.00, percentage: 0.453, colorHex: '#8D6E63', subcategories: [] },
        { categoryId: 1,  categoryName: 'Groceries',     amount: -127.45,  percentage: 0.032, colorHex: '#66BB6A', subcategories: [] },
        { categoryId: 9,  categoryName: 'Travel',        amount: -594.00,  percentage: 0.150, colorHex: '#FF7043', subcategories: [] },
        { categoryId: 5,  categoryName: 'Transport',     amount: -138.50,  percentage: 0.035, colorHex: '#26C6DA', subcategories: [] },
        { categoryId: 7,  categoryName: 'Food & Drink',  amount: -218.80,  percentage: 0.055, colorHex: '#EF5350', subcategories: [] },
        { categoryId: 6,  categoryName: 'Shopping',      amount: -89.99,   percentage: 0.023, colorHex: '#7E57C2', subcategories: [] },
        { categoryId: 4,  categoryName: 'Utilities',     amount: -94.20,   percentage: 0.024, colorHex: '#FFA726', subcategories: [] },
        { categoryId: 3,  categoryName: 'Entertainment', amount: -25.98,   percentage: 0.007, colorHex: '#EC407A', subcategories: [] },
        { categoryId: 14, categoryName: 'Subscriptions', amount: -9.99,    percentage: 0.003, colorHex: '#78909C', subcategories: [] },
        { categoryId: 15, categoryName: 'Pets',          amount: -60.00,   percentage: 0.015, colorHex: '#A5D6A7', subcategories: [] },
        { categoryId: 8,  categoryName: 'Health',        amount: -45.00,   percentage: 0.011, colorHex: '#26A69A', subcategories: [] },
      ]
    }
  },
  {
    cardId: 'dc-cattree', cardType: 'CATEGORY_TREEMAP', title: 'Category Treemap',
    order: 10, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: {},
    data: {
      type: 'CategoryBreakdown', displayCurrency: 'USD',
      items: [
        { categoryId: 10, categoryName: 'Rent',          amount: -1800.00, percentage: 45.3, colorHex: '#8D6E63', subcategories: [] },
        { categoryId: 9,  categoryName: 'Travel',        amount: -594.00,  percentage: 15.0, colorHex: '#FF7043', subcategories: [] },
        { categoryId: 7,  categoryName: 'Food & Drink',  amount: -218.80,  percentage: 5.5,  colorHex: '#EF5350', subcategories: [] },
        { categoryId: 5,  categoryName: 'Transport',     amount: -138.50,  percentage: 3.5,  colorHex: '#26C6DA', subcategories: [] },
        { categoryId: 4,  categoryName: 'Utilities',     amount: -94.20,   percentage: 2.4,  colorHex: '#FFA726', subcategories: [] },
        { categoryId: 6,  categoryName: 'Shopping',      amount: -89.99,   percentage: 2.3,  colorHex: '#7E57C2', subcategories: [] },
        { categoryId: 15, categoryName: 'Pets',          amount: -60.00,   percentage: 1.5,  colorHex: '#A5D6A7', subcategories: [] },
        { categoryId: 8,  categoryName: 'Health',        amount: -45.00,   percentage: 1.1,  colorHex: '#26A69A', subcategories: [] },
        { categoryId: 1,  categoryName: 'Groceries',     amount: -127.45,  percentage: 3.2,  colorHex: '#66BB6A', subcategories: [] },
        { categoryId: 3,  categoryName: 'Entertainment', amount: -25.98,   percentage: 0.7,  colorHex: '#EC407A', subcategories: [] },
        { categoryId: 14, categoryName: 'Subscriptions', amount: -9.99,    percentage: 0.3,  colorHex: '#78909C', subcategories: [] },
      ]
    }
  },
  {
    cardId: 'dc-cfcomp', cardType: 'CASHFLOW_COMPARISON', title: 'Cash Flow Comparison',
    order: 11, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { selectedMetric: 'net', showCurrent: true, showPrevious: true, showLastYear: true, isCumulative: false },
    data: {
      type: 'CashFlowComparison',
      selectedMetric: 'net', showCurrent: true, showPrevious: true, showLastYear: true, isCumulative: false,
      current:  LABELS_6M.map((l,i) => mkBucket(l, 5000+i*200, -(3200+i*100))),
      previous: LABELS_6M.map((l,i) => mkBucket(l, 4800+i*100, -(3400+i*50))),
      lastYear: LABELS_6M.map((l,i) => mkBucket(l, 4200+i*80,  -(3000+i*60))),
    }
  },
  {
    cardId: 'dc-iebar', cardType: 'INCOME_EXPENSE_BAR', title: 'Income & Expense',
    order: 12, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { isCumulative: false },
    data: {
      type: 'IncomeExpenseBar', baseCurrency: 'USD', granularity: 'WEEK', isCumulative: false, todayBucketIndex: 3,
      buckets: [
        { label: 'W1', income: 0,      expense: -980  },
        { label: 'W2', income: 5200,   expense: -1240 },
        { label: 'W3', income: 800,    expense: -890  },
        { label: 'W4', income: 0,      expense: -860  },
      ]
    }
  },
  {
    cardId: 'dc-cattrend', cardType: 'CATEGORY_SPENDING_TREND', title: 'Category Spending Trend',
    order: 13, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { selectedNatures: [], isCumulative: false },
    data: {
      type: 'CategorySpendingTrend', baseCurrency: 'USD', isCumulative: false, selectedNatures: [],
      activeCategories: [
        { categoryId: 10, categoryName: 'Rent',       amount: -1800, colorHex: '#8D6E63', nature: 'MUST' },
        { categoryId: 1,  categoryName: 'Groceries',  amount: -127,  colorHex: '#66BB6A', nature: 'NEED' },
        { categoryId: 7,  categoryName: 'Food & Drink',amount: -218, colorHex: '#EF5350', nature: 'WANT' },
      ],
      buckets: LABELS_6M.map((l, i) => ({
        label: l,
        categories: [
          { nature: 'MUST', amount: 1800+i*20 },
          { nature: 'NEED', amount: 400+i*15  },
          { nature: 'WANT', amount: 300+i*30  },
        ]
      }))
    }
  },
  {
    cardId: 'dc-tagplot', cardType: 'TAG_UPSET_PLOT', title: 'Tag Analysis',
    order: 14, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { selectedMetric: 'EXPENSE' },
    data: {
      type: 'TagUpSetPlot', selectedMetric: 'EXPENSE', baseCurrency: 'USD',
      sets: [
        { tagName: 'recurring',       income: 5200,  expense: -1925.98, transactionCount: 5 },
        { tagName: 'personal',        income: 0,     expense: -127.45,  transactionCount: 2 },
        { tagName: 'work',            income: 6000,  expense: 0,        transactionCount: 2 },
        { tagName: 'travel',          income: 0,     expense: -594.00,  transactionCount: 3 },
        { tagName: 'tax-deductible',  income: 5200,  expense: 0,        transactionCount: 1 },
        { tagName: 'reimbursable',    income: 0,     expense: -89.99,   transactionCount: 1 },
        { tagName: 'health',          income: 0,     expense: -45.00,   transactionCount: 1 },
      ],
      intersections: [
        { tags: ['work', 'tax-deductible'],  income: 5200, expense: 0,  transactionCount: 1 },
        { tags: ['recurring', 'personal'],   income: 0,    expense: -127.45, transactionCount: 1 },
        { tags: ['travel', 'reimbursable'],  income: 0,    expense: -89.99,  transactionCount: 1 },
      ]
    }
  },
];

MOCK_DATA['/api/dashboard'] = { success: true, data: DASH_CARDS };

// Per-card refresh endpoint
DASH_CARDS.forEach(c => {
  MOCK_DATA[`/api/dashboard/cards/${c.cardId}`] = { success: true, data: c };
});

/* ────────────── ANALYTICS ────────────── */
// Analytics uses a subset of cards, organised by tab
const ANALYTICS_CARDS = [
  { ...DASH_CARDS.find(c => c.cardId === 'dc-networth'),  cardId: 'ac-networth', order: 0 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-cashflow'),  cardId: 'ac-cashflow', order: 1 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-recent'),    cardId: 'ac-recent',   order: 2 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-currency'),  cardId: 'ac-currency', cardType: 'CURRENCY_BALANCES', order: 3 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-accounts'),  cardId: 'ac-accounts', order: 4 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-hero'),      cardId: 'ac-hero',     cardType: 'ACCOUNT_HERO', order: 5 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-budgets'),   cardId: 'ac-budgets',  order: 6 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-planned'),   cardId: 'ac-planned',  order: 7 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-loans'),     cardId: 'ac-loans',    order: 8 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-catpie'),    cardId: 'ac-catpie',   order: 9 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-cattree'),   cardId: 'ac-cattree',  order: 10 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-tagplot'),   cardId: 'ac-tagplot',  order: 11 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-cfcomp'),    cardId: 'ac-cfcomp',   order: 12 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-iebar'),     cardId: 'ac-iebar',    order: 13 },
  { ...DASH_CARDS.find(c => c.cardId === 'dc-cattrend'),  cardId: 'ac-cattrend', order: 14 },
];

MOCK_DATA['/api/analytics'] = {
  success: true,
  data: {
    cards: ANALYTICS_CARDS,
    commonFilterEnabled: false,
    screenFilter: { timelineType: 'MONTH', accountIds: [], categoryIds: [], transactionTypes: [], tags: [], currencies: [], statuses: [], paymentMethods: [], queryNote: '', queryPayee: '', minAmount: null, maxAmount: null, excludeFuture: true },
  }
};

ANALYTICS_CARDS.forEach(c => {
  MOCK_DATA[`/api/dashboard/cards/${c.cardId}`] = { success: true, data: c };
});

} // end guard

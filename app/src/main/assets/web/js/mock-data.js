/* Mock fixture data — only active when ?mock=true */
/* Aligned with DatabaseSeeder CSV data for accurate functional testing. */
if (typeof window.MOCK_DATA === 'undefined') {
window.MOCK_DATA = {};

/* ────────────── SETTINGS ────────────── */
MOCK_DATA['/api/settings'] = {
  success: true,
  data: {
    currentProfileId: 1,
    profileName: 'Default Profile',
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
  { id: 'acc_cash',    name: 'Cash Wallet',        currency: 'USD', type: 'CASH',           balance: 515.00,      colorHex: '#4CAF50', income: 300.00,   expense: 785.00,   transferIn: 100.00,   transferOut: 700.00,  distribution: { USD: 515.00 } },
  { id: 'acc_bank',    name: 'Main Bank',           currency: 'USD', type: 'BANK',           balance: 8420.00,     colorHex: '#2196F3', income: 22050.00, expense: 12630.00, transferIn: 550.00,   transferOut: 3550.00, distribution: { USD: 7500.00, INR: 120000.00, EUR: 1500.00 } },
  { id: 'acc_savings', name: 'Savings',             currency: 'USD', type: 'SAVINGS',        balance: 18000.00,    colorHex: '#9C27B0', income: 5200.00,  expense: 500.00,   transferIn: 3000.00,  transferOut: 1000.00, distribution: { USD: 18000.00 } },
  { id: 'acc_credit',  name: 'Credit Card',         currency: 'USD', type: 'CREDIT',         balance: -2850.00,    colorHex: '#F44336', income: 350.00,   expense: 4200.00,  transferIn: 350.00,   transferOut: 0,       distribution: { USD: -2850.00 }, creditLimit: 10000 },
  { id: 'acc_invest',  name: 'Investment',          currency: 'USD', type: 'INVESTMENT',     balance: 14200.00,    colorHex: '#FF9800', income: 4250.00,  expense: 0,        transferIn: 2000.00,  transferOut: 0,       distribution: { USD: 14200.00 } },
  { id: 'acc_jpy',     name: 'Japan Travel Card',   currency: 'JPY', type: 'BANK',           balance: 115400.00,   colorHex: '#FF5722', income: 0,        expense: 8500.00,  transferIn: 29400.00, transferOut: 0,       distribution: { JPY: 115400.00 } },
  { id: 'acc_sgd',     name: 'DBS Bank',            currency: 'SGD', type: 'BANK',           balance: 5400.00,     colorHex: '#E91E63', income: 0,        expense: 0,        transferIn: 400.00,   transferOut: 0,       distribution: { SGD: 5400.00 } },
  { id: 'acc_idr',     name: 'GoPay IDR',           currency: 'IDR', type: 'DIGITAL_WALLET', balance: 9220000.00,  colorHex: '#00C853', income: 0,        expense: 0,        transferIn: 0,        transferOut: 780000.00, distribution: { IDR: 9220000.00 } },
];

MOCK_DATA['/api/accounts'] = { success: true, data: MOCK_ACCOUNTS };

/* ────────────── CATEGORIES ────────────── */
const C = {
  food: '#f57c00', transport: '#1565c0', shopping: '#7b1fa2', housing: '#00695c',
  health: '#c62828', entertainment: '#6d28da', travel: '#0277bd', education: '#1a237e',
  finance: '#33691e', subscriptions: '#311b92', gifts: '#880e4f', business: '#37474f',
  income: '#1b5e20'
};
const MOCK_CATEGORIES = [
  // Roots
  { id: 1,   name: 'Food & Drink',          colorHex: C.food,          type: 'EXPENSE', parentId: null },
  { id: 2,   name: 'Transport',             colorHex: C.transport,     type: 'EXPENSE', parentId: null },
  { id: 3,   name: 'Shopping',              colorHex: C.shopping,      type: 'EXPENSE', parentId: null },
  { id: 4,   name: 'Housing',               colorHex: C.housing,       type: 'EXPENSE', parentId: null },
  { id: 5,   name: 'Health',                colorHex: C.health,        type: 'EXPENSE', parentId: null },
  { id: 6,   name: 'Entertainment',         colorHex: C.entertainment, type: 'EXPENSE', parentId: null },
  { id: 7,   name: 'Travel',                colorHex: C.travel,        type: 'EXPENSE', parentId: null },
  { id: 8,   name: 'Education',             colorHex: C.education,     type: 'EXPENSE', parentId: null },
  { id: 9,   name: 'Finance',               colorHex: C.finance,       type: 'EXPENSE', parentId: null },
  { id: 10,  name: 'Subscriptions',         colorHex: C.subscriptions, type: 'EXPENSE', parentId: null },
  { id: 11,  name: 'Gifts & Charity',       colorHex: C.gifts,         type: 'EXPENSE', parentId: null },
  { id: 12,  name: 'Business',              colorHex: C.business,      type: 'EXPENSE', parentId: null },
  { id: 13,  name: 'Income',                colorHex: C.income,        type: 'INCOME',  parentId: null },
  // Food & Drink subcategories
  { id: 101, name: 'Groceries',             colorHex: C.food,          type: 'EXPENSE', parentId: 1 },
  { id: 102, name: 'Dining Out',            colorHex: C.food,          type: 'EXPENSE', parentId: 1 },
  { id: 103, name: 'Coffee & Snacks',       colorHex: C.food,          type: 'EXPENSE', parentId: 1 },
  { id: 104, name: 'Bars & Nightlife',      colorHex: C.food,          type: 'EXPENSE', parentId: 1 },
  { id: 105, name: 'Food Delivery',         colorHex: C.food,          type: 'EXPENSE', parentId: 1 },
  // Transport subcategories
  { id: 110, name: 'Own Vehicle',           colorHex: C.transport,     type: 'EXPENSE', parentId: 2 },
  { id: 111, name: 'Public Transit',        colorHex: C.transport,     type: 'EXPENSE', parentId: 2 },
  { id: 112, name: 'Ride & Taxi',           colorHex: C.transport,     type: 'EXPENSE', parentId: 2 },
  { id: 113, name: 'Cycling',               colorHex: C.transport,     type: 'EXPENSE', parentId: 2 },
  // Shopping subcategories
  { id: 120, name: 'Clothing & Apparel',    colorHex: C.shopping,      type: 'EXPENSE', parentId: 3 },
  { id: 121, name: 'Electronics',           colorHex: C.shopping,      type: 'EXPENSE', parentId: 3 },
  { id: 122, name: 'Home & Living',         colorHex: C.shopping,      type: 'EXPENSE', parentId: 3 },
  { id: 123, name: 'Books & Stationery',    colorHex: C.shopping,      type: 'EXPENSE', parentId: 3 },
  { id: 124, name: 'Online Shopping',       colorHex: C.shopping,      type: 'EXPENSE', parentId: 3 },
  // Housing subcategories
  { id: 130, name: 'Rent',                  colorHex: C.housing,       type: 'EXPENSE', parentId: 4 },
  { id: 131, name: 'Utilities',             colorHex: C.housing,       type: 'EXPENSE', parentId: 4 },
  { id: 132, name: 'Home Services',         colorHex: C.housing,       type: 'EXPENSE', parentId: 4 },
  { id: 133, name: 'Furnishing',            colorHex: C.housing,       type: 'EXPENSE', parentId: 4 },
  // Health subcategories
  { id: 140, name: 'Medical',               colorHex: C.health,        type: 'EXPENSE', parentId: 5 },
  { id: 141, name: 'Pharmacy',              colorHex: C.health,        type: 'EXPENSE', parentId: 5 },
  { id: 142, name: 'Fitness',               colorHex: C.health,        type: 'EXPENSE', parentId: 5 },
  { id: 143, name: 'Mental & Wellbeing',    colorHex: C.health,        type: 'EXPENSE', parentId: 5 },
  { id: 144, name: 'Personal Care',         colorHex: C.health,        type: 'EXPENSE', parentId: 5 },
  // Entertainment subcategories
  { id: 150, name: 'Movies & Theater',      colorHex: C.entertainment, type: 'EXPENSE', parentId: 6 },
  { id: 151, name: 'Music',                 colorHex: C.entertainment, type: 'EXPENSE', parentId: 6 },
  { id: 152, name: 'Gaming',                colorHex: C.entertainment, type: 'EXPENSE', parentId: 6 },
  { id: 153, name: 'Events & Shows',        colorHex: C.entertainment, type: 'EXPENSE', parentId: 6 },
  { id: 154, name: 'Hobbies',               colorHex: C.entertainment, type: 'EXPENSE', parentId: 6 },
  // Travel subcategories
  { id: 160, name: 'Flights',               colorHex: C.travel,        type: 'EXPENSE', parentId: 7 },
  { id: 161, name: 'Accommodation',         colorHex: C.travel,        type: 'EXPENSE', parentId: 7 },
  { id: 162, name: 'Ground Transport',      colorHex: C.travel,        type: 'EXPENSE', parentId: 7 },
  { id: 163, name: 'Activities',            colorHex: C.travel,        type: 'EXPENSE', parentId: 7 },
  { id: 164, name: 'Travel Essentials',     colorHex: C.travel,        type: 'EXPENSE', parentId: 7 },
  // Education subcategories
  { id: 170, name: 'Tuition & Fees',        colorHex: C.education,     type: 'EXPENSE', parentId: 8 },
  { id: 171, name: 'Online Learning',       colorHex: C.education,     type: 'EXPENSE', parentId: 8 },
  { id: 172, name: 'Books & Supplies',      colorHex: C.education,     type: 'EXPENSE', parentId: 8 },
  { id: 173, name: 'Childcare',             colorHex: C.education,     type: 'EXPENSE', parentId: 8 },
  // Finance subcategories
  { id: 180, name: 'Savings',               colorHex: C.finance,       type: 'EXPENSE', parentId: 9 },
  { id: 181, name: 'Investments',           colorHex: C.finance,       type: 'EXPENSE', parentId: 9 },
  { id: 182, name: 'Insurance',             colorHex: C.finance,       type: 'EXPENSE', parentId: 9 },
  { id: 183, name: 'Taxes & Fees',          colorHex: C.finance,       type: 'EXPENSE', parentId: 9 },
  { id: 184, name: 'Loans & Debt',          colorHex: C.finance,       type: 'EXPENSE', parentId: 9 },
  // Subscriptions subcategories
  { id: 185, name: 'Video Streaming',       colorHex: C.subscriptions, type: 'EXPENSE', parentId: 10 },
  { id: 186, name: 'Music & Audio',         colorHex: C.subscriptions, type: 'EXPENSE', parentId: 10 },
  { id: 187, name: 'Software Subscriptions',colorHex: C.subscriptions, type: 'EXPENSE', parentId: 10 },
  { id: 188, name: 'News & Reading',        colorHex: C.subscriptions, type: 'EXPENSE', parentId: 10 },
  { id: 189, name: 'Memberships',           colorHex: C.subscriptions, type: 'EXPENSE', parentId: 10 },
  // Gifts & Charity subcategories
  { id: 190, name: 'Gifts',                 colorHex: C.gifts,         type: 'EXPENSE', parentId: 11 },
  { id: 191, name: 'Donations',             colorHex: C.gifts,         type: 'EXPENSE', parentId: 11 },
  { id: 192, name: 'Family Support',        colorHex: C.gifts,         type: 'EXPENSE', parentId: 11 },
  // Business subcategories
  { id: 193, name: 'Office & Supplies',     colorHex: C.business,      type: 'EXPENSE', parentId: 12 },
  { id: 194, name: 'Marketing',             colorHex: C.business,      type: 'EXPENSE', parentId: 12 },
  { id: 195, name: 'Professional Services', colorHex: C.business,      type: 'EXPENSE', parentId: 12 },
  { id: 196, name: 'Client Entertainment',  colorHex: C.business,      type: 'EXPENSE', parentId: 12 },
  // Income subcategories
  { id: 197, name: 'Employment',            colorHex: C.income,        type: 'INCOME',  parentId: 13 },
  { id: 198, name: 'Freelance',             colorHex: C.income,        type: 'INCOME',  parentId: 13 },
  { id: 199, name: 'Investment Returns',    colorHex: C.income,        type: 'INCOME',  parentId: 13 },
  { id: 200, name: 'Other Income',          colorHex: C.income,        type: 'INCOME',  parentId: 13 },
];

MOCK_DATA['/api/categories'] = { success: true, data: MOCK_CATEGORIES };

/* ────────────── TAGS ────────────── */
MOCK_DATA['/api/tags'] = {
  success: true,
  data: [
    'Personal','Travel','Work','Housing','Health','Social','Subscription','Emergency',
    'Gifts','Food','Shopping','Project','Japan','Vacation','Essential','Salary','Luxury',
    'Education','Tax','Cashback','Transport','Entertainment','Investments','Misc','Fitness',
    'Dining','Groceries','Business','Bills','Electronics','Utilities','Rent','Freelance',
    'Savings','Auto','Maintenance','Insurance','Interest','Fees','Adjustment'
  ].map(name => ({ name }))
};

/* ────────────── TRANSACTIONS ────────────── */
const D = Date.now();
const DAY = 86400000;
const ago = d => D - d * DAY;

// paymentMethod normalizer
const pm = s => {
  if (!s) return 'OTHER';
  const u = s.toUpperCase().replace(/\s+/g,'_');
  if (u === 'CASH') return 'CASH';
  if (u === 'DEBIT_CARD') return 'DEBIT_CARD';
  if (u === 'CREDIT_CARD') return 'CREDIT_CARD';
  if (u === 'BANK_TRANSFER') return 'BANK_TRANSFER';
  if (u === 'DIGITAL_WALLET' || u === 'E-WALLET' || u === 'MOBILE_PAYMENT') return 'DIGITAL_WALLET';
  if (u === 'OTHER') return 'OTHER';
  return 'OTHER';
};

// Lookup helpers
const catName = id => (MOCK_CATEGORIES.find(c => c.id === id) || {}).name || null;
const accName  = id => (MOCK_ACCOUNTS.find(a => a.id === id) || {}).name || null;
const accCurr  = id => (MOCK_ACCOUNTS.find(a => a.id === id) || {}).currency || 'USD';

window.MOCK_TRANSACTIONS = [
  /* ── daysAgo transactions (rows 9–36) ── */
  { id: 9,    title: 'Monthly Salary',            amount: 5250.00, currency: 'USD', type: 'INCOME',   date: ago(60), categoryId: 197, categoryName: 'Salary',          accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Monthly salary deposit',      tags: ['Work','Salary','Essential'] },
  { id: 10,   title: 'Rent Payment',              amount: 1850.00, currency: 'USD', type: 'EXPENSE',  date: ago(58), categoryId: 130, categoryName: 'Rent',            accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Property Management', note: 'Monthly rent payment',        tags: ['Housing','Essential','Rent','Bills'] },
  { id: 11,   title: 'Whole Foods Groceries',     amount: 142.50,  currency: 'USD', type: 'EXPENSE',  date: ago(55), categoryId: 101, categoryName: 'Groceries',       accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: 'Whole Foods',        note: 'Weekly groceries',            tags: ['Food','Essential','Groceries'] },
  { id: 12,   title: 'Morning Coffee',            amount: 6.50,    currency: 'USD', type: 'EXPENSE',  date: ago(54), categoryId: 103, categoryName: 'Coffee & Snacks', accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Starbucks',          note: 'Latte',                       tags: ['Food','Social','Dining'],    latitude: 40.7128, longitude: -74.006 },
  { id: 13,   title: 'Gas Station',               amount: 55.00,   currency: 'USD', type: 'EXPENSE',  date: ago(53), categoryId: 110, categoryName: 'Fuel',            accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Shell',              note: 'Full tank',                   tags: ['Transport','Essential','Auto'] },
  { id: 14,   title: 'Netflix Subscription',      amount: 15.99,   currency: 'USD', type: 'EXPENSE',  date: ago(52), categoryId: 185, categoryName: 'Video Streaming', accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Netflix',            note: 'Monthly subscription',        tags: ['Subscription','Entertainment'] },
  { id: 15,   title: 'Phone Bill',                amount: 65.00,   currency: 'USD', type: 'EXPENSE',  date: ago(51), categoryId: 131, categoryName: 'Utilities',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Verizon',            note: 'Monthly mobile bill',         tags: ['Housing','Essential','Bills','Utilities'] },
  { id: 16,   title: 'Gym Membership',            amount: 49.99,   currency: 'USD', type: 'EXPENSE',  date: ago(50), categoryId: 142, categoryName: 'Fitness',         accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'LA Fitness',         note: 'Monthly membership',          tags: ['Health','Essential','Fitness'] },
  { id: 17,   title: 'Dinner with Friends',       amount: 85.00,   currency: 'USD', type: 'EXPENSE',  date: ago(49), categoryId: 102, categoryName: 'Dining Out',      accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Italian Bistro',     note: 'Weekend dinner',              tags: ['Social','Food','Dining'] },
  { id: 18,   title: 'Amazon Purchase',           amount: 45.99,   currency: 'USD', type: 'EXPENSE',  date: ago(48), categoryId: 124, categoryName: 'Online Shopping', accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Amazon',             note: 'Books',                       tags: ['Shopping','Personal','Education'] },
  { id: 19,   title: 'Freelance Payment',         amount: 1200.00, currency: 'USD', type: 'INCOME',   date: ago(47), categoryId: 198, categoryName: 'Freelance',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Client A',           note: 'Web dev project',             tags: ['Work','Project','Freelance','Business'] },
  { id: 20,   title: 'Lunch at Work',             amount: 15.50,   currency: 'USD', type: 'EXPENSE',  date: ago(46), categoryId: 102, categoryName: 'Dining Out',      accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Cafeteria',          note: 'Daily lunch',                 tags: ['Work','Food','Dining'] },
  { id: 21,   title: 'Gas Station',               amount: 52.00,   currency: 'USD', type: 'EXPENSE',  date: ago(45), categoryId: 110, categoryName: 'Fuel',            accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Exxon',              note: 'Weekly gas',                  tags: ['Transport','Essential','Auto'] },
  { id: 22,   title: 'Pharmacy',                  amount: 35.00,   currency: 'USD', type: 'EXPENSE',  date: ago(44), categoryId: 141, categoryName: 'Pharmacy',        accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: 'CVS',                note: 'Vitamins',                    tags: ['Health','Essential'] },
  { id: 23,   title: 'Weekend Getaway Hotel',     amount: 220.00,  currency: 'USD', type: 'EXPENSE',  date: ago(43), categoryId: 161, categoryName: 'Accommodation',   accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Marriott',           note: 'One night stay',              tags: ['Travel','Vacation','Luxury'] },
  { id: 24,   title: 'Uber Ride',                 amount: 24.00,   currency: 'USD', type: 'EXPENSE',  date: ago(42), categoryId: 112, categoryName: 'Ride & Taxi',     accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Uber',               note: 'Ride to hotel',               tags: ['Travel','Transport'] },
  { id: 25,   title: 'Consulting Income',         amount: 3500.00, currency: 'USD', type: 'INCOME',   date: ago(41), categoryId: 197, categoryName: 'Employment',      accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Consulting Group',   note: 'Project milestone',           tags: ['Work','Project','Business','Freelance'] },
  { id: 26,   title: 'Electricity Bill',          amount: 135.00,  currency: 'USD', type: 'EXPENSE',  date: ago(40), categoryId: 131, categoryName: 'Utilities',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Power Co',           note: 'Monthly usage',               tags: ['Housing','Essential','Bills','Utilities'] },
  { id: 27,   title: 'Internet Bill',             amount: 79.99,   currency: 'USD', type: 'EXPENSE',  date: ago(39), categoryId: 131, categoryName: 'Utilities',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'ISP',                note: 'Fiber internet',              tags: ['Housing','Essential','Bills','Utilities'] },
  { id: 28,   title: 'Spotify',                   amount: 10.99,   currency: 'USD', type: 'EXPENSE',  date: ago(38), categoryId: 186, categoryName: 'Music & Audio',   accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Spotify',            note: 'Music subscription',          tags: ['Subscription','Entertainment'] },
  { id: 29,   title: 'Weekly Groceries',          amount: 95.20,   currency: 'USD', type: 'EXPENSE',  date: ago(37), categoryId: 101, categoryName: 'Groceries',       accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: "Trader Joe's",       note: 'Groceries',                   tags: ['Food','Essential','Groceries'] },
  { id: 31,   title: 'INR Salary Deposit',        amount: 125000.00,currency:'INR', type: 'INCOME',   date: ago(35), categoryId: 197, categoryName: 'Salary',          accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'India Corp',         note: 'Monthly INR salary',          tags: ['Work','Salary'] },
  { id: 34,   title: 'European Client Payment',   amount: 1500.00, currency: 'EUR', type: 'INCOME',   date: ago(32), categoryId: 198, categoryName: 'Freelance',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Client B',           note: 'EUR project payment',         tags: ['Work','Project','Freelance','Business'] },
  { id: 35,   title: 'Paris Business Lunch',      amount: 45.00,   currency: 'EUR', type: 'EXPENSE',  date: ago(31), categoryId: 102, categoryName: 'Dining Out',      accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: 'Le Bistro',          note: 'Lunch in Paris',              tags: ['Travel','Work','Dining'],   latitude: 48.8566, longitude: 2.3522 },

  /* ── rows 101–112 (older, daysAgo) ── */
  { id: 101,  title: 'Salary Deposit',            amount: 5250.00, currency: 'USD', type: 'INCOME',   date: ago(29), categoryId: 197, categoryName: 'Salary',          accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Monthly salary',              tags: ['Work','Salary','Essential'] },
  { id: 102,  title: 'Rent Payment',              amount: 1850.00, currency: 'USD', type: 'EXPENSE',  date: ago(28), categoryId: 130, categoryName: 'Rent',            accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Property Management', note: 'Monthly rent',               tags: ['Housing','Essential','Rent','Bills'] },
  { id: 103,  title: 'Whole Foods',               amount: 135.20,  currency: 'USD', type: 'EXPENSE',  date: ago(25), categoryId: 101, categoryName: 'Groceries',       accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: 'Whole Foods',        note: 'Weekly food',                 tags: ['Food','Essential','Groceries'] },
  { id: 105,  title: 'Movie Tickets',             amount: 30.00,   currency: 'USD', type: 'EXPENSE',  date: ago(22), categoryId: 150, categoryName: 'Movies & Theater',accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: 'AMC',                note: 'Weekend movie',               tags: ['Entertainment','Social'] },
  { id: 106,  title: 'Dinner Date',               amount: 110.00,  currency: 'USD', type: 'EXPENSE',  date: ago(21), categoryId: 102, categoryName: 'Dining Out',      accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Steakhouse',         note: 'Nice dinner',                 tags: ['Social','Food','Dining','Luxury'] },
  { id: 107,  title: 'Adobe CC',                  amount: 54.99,   currency: 'USD', type: 'EXPENSE',  date: ago(20), categoryId: 187, categoryName: 'Software Subscriptions', accountId: 'acc_credit', accountName: 'Credit Card',   status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Adobe',              note: 'Subscription',                tags: ['Work','Subscription','Project','Business'] },
  { id: 109,  title: 'Haircut',                   amount: 40.00,   currency: 'USD', type: 'EXPENSE',  date: ago(18), categoryId: 144, categoryName: 'Personal Care',   accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Barber',             note: 'Monthly haircut',             tags: ['Personal','Health'] },
  { id: 110,  title: 'Amazon Household',          amount: 65.00,   currency: 'USD', type: 'EXPENSE',  date: ago(17), categoryId: 124, categoryName: 'Online Shopping', accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Amazon',             note: 'Supplies',                    tags: ['Shopping','Essential','Housing'] },
  { id: 111,  title: 'Water Bill',                amount: 40.00,   currency: 'USD', type: 'EXPENSE',  date: ago(16), categoryId: 131, categoryName: 'Utilities',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Water Dept',         note: 'Monthly bill',                tags: ['Housing','Essential','Bills','Utilities'] },
  { id: 112,  title: 'Electric Bill',             amount: 125.00,  currency: 'USD', type: 'EXPENSE',  date: ago(15), categoryId: 131, categoryName: 'Utilities',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Power Co',           note: 'Power',                       tags: ['Housing','Essential','Bills','Utilities'] },

  /* ── special cases (201–205) ── */
  { id: 201,  title: 'Car Loan Payment',          amount: 450.00,  currency: 'USD', type: 'EXPENSE',  date: ago(10), categoryId: 184, categoryName: 'Loans & Debt',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Chase Auto',         note: 'Monthly installment',         tags: ['CarLoan'] },
  { id: 202,  title: 'Sushi Dinner Tokyo',        amount: 8500.00, currency: 'JPY', type: 'EXPENSE',  date: ago(5),  categoryId: 102, categoryName: 'Dining Out',      accountId: 'acc_jpy',     accountName: 'Japan Travel Card',   status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Kyubey Sushi',       note: 'Amazing sushi',               tags: ['Travel','Food'],            latitude: 35.6762, longitude: 139.7646 },
  { id: 203,  title: 'John Repayment',            amount: 100.00,  currency: 'USD', type: 'INCOME',   date: ago(2),  categoryId: 200, categoryName: 'Other Income',    accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'John Doe',           note: 'Partial repayment of loan',   tags: [] },
  { id: 204,  title: 'Berlin Hotel',              amount: 120.00,  currency: 'EUR', type: 'EXPENSE',  date: ago(12), categoryId: 161, categoryName: 'Accommodation',   accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Hilton Berlin',      note: 'Business trip',               tags: ['Travel'],                   latitude: 52.52, longitude: 13.405 },
  { id: 205,  title: 'Bangalore Dinner',          amount: 2500.00, currency: 'INR', type: 'EXPENSE',  date: ago(4),  categoryId: 102, categoryName: 'Dining Out',      accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'DIGITAL_WALLET', payee: 'Toit',             note: 'Dinner in Bangalore',         tags: ['Social'],                   latitude: 12.9716, longitude: 77.5946 },

  /* ── transfer pairs ── */
  { id: 1001, title: 'Cash Deposit to Bank',      amount: 500.00,  currency: 'USD', type: 'TRANSFER', date: ago(25), categoryId: null, categoryName: null,             accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: null,                 note: 'Depositing cash at the bank', tags: [] },
  { id: 1002, title: 'Cash Deposit to Bank',      amount: 500.00,  currency: 'USD', type: 'TRANSFER', date: ago(25), categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Depositing cash at the bank', tags: [] },
  { id: 1003, title: 'Monthly Savings Transfer',  amount: 1000.00, currency: 'USD', type: 'TRANSFER', date: ago(20), categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Routine savings deposit',     tags: ['Savings'] },
  { id: 1004, title: 'Monthly Savings Transfer',  amount: 1000.00, currency: 'USD', type: 'TRANSFER', date: ago(20), categoryId: null, categoryName: null,             accountId: 'acc_savings', accountName: 'Savings',             status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Routine savings deposit',     tags: ['Savings'] },
  { id: 1005, title: 'Credit Card Payment',       amount: 350.00,  currency: 'USD', type: 'TRANSFER', date: ago(14), categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Paying off credit card balance', tags: [] },
  { id: 1006, title: 'Credit Card Payment',       amount: 350.00,  currency: 'USD', type: 'TRANSFER', date: ago(14), categoryId: null, categoryName: null,             accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Paying off credit card balance', tags: [] },
  { id: 1007, title: 'Investment Top-up',         amount: 2000.00, currency: 'USD', type: 'TRANSFER', date: ago(10), categoryId: null, categoryName: null,             accountId: 'acc_savings', accountName: 'Savings',             status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Adding funds to investment account', tags: ['Investment'] },
  { id: 1008, title: 'Investment Top-up',         amount: 2000.00, currency: 'USD', type: 'TRANSFER', date: ago(10), categoryId: null, categoryName: null,             accountId: 'acc_invest',  accountName: 'Investment',          status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Adding funds to investment account', tags: ['Investment'] },
  { id: 1009, title: 'USD to JPY Exchange',       amount: 200.00,  currency: 'USD', type: 'TRANSFER', date: ago(18), categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Currency exchange before Japan trip', tags: ['Travel'] },
  { id: 1010, title: 'USD to JPY Exchange',       amount: 29400.00,currency: 'JPY', type: 'TRANSFER', date: ago(18), categoryId: null, categoryName: null,             accountId: 'acc_jpy',     accountName: 'Japan Travel Card',   status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Currency exchange before Japan trip', tags: ['Travel'] },
  { id: 1011, title: 'USD to SGD Transfer',       amount: 300.00,  currency: 'USD', type: 'TRANSFER', date: ago(12), categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Top up SGD account',          tags: [] },
  { id: 1012, title: 'USD to SGD Transfer',       amount: 400.00,  currency: 'SGD', type: 'TRANSFER', date: ago(12), categoryId: null, categoryName: null,             accountId: 'acc_sgd',     accountName: 'DBS Bank',            status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Top up SGD account',          tags: [] },
  { id: 1013, title: 'IDR to USD Repatriation',   amount: 780000.00,currency:'IDR', type: 'TRANSFER', date: ago(6),  categoryId: null, categoryName: null,             accountId: 'acc_idr',     accountName: 'GoPay IDR',           status: 'CLEARED',    paymentMethod: 'DIGITAL_WALLET', payee: null,                note: 'Converting leftover IDR',     tags: [] },
  { id: 1014, title: 'IDR to USD Repatriation',   amount: 50.00,   currency: 'USD', type: 'TRANSFER', date: ago(6),  categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Converting leftover IDR',     tags: [] },
  { id: 1015, title: 'Savings to Checking',       amount: 100.00,  currency: 'USD', type: 'TRANSFER', date: ago(5),  categoryId: null, categoryName: null,             accountId: 'acc_savings', accountName: 'Savings',             status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Savings to Checking',         tags: ['Savings'] },
  { id: 1016, title: 'Savings to Checking',       amount: 100.00,  currency: 'USD', type: 'TRANSFER', date: ago(5),  categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Savings to Checking',         tags: ['Savings'] },

  /* ── special status transactions ── */
  { id: 2000, title: 'Unpaid Invoice',            amount: 500.00,  currency: 'USD', type: 'INCOME',   date: ago(2),  categoryId: 198, categoryName: 'Freelance',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'UNCLEARED',  paymentMethod: 'BANK_TRANSFER', payee: 'Client Z',           note: 'Pending payment',             tags: ['Project'] },
  { id: 2001, title: 'Returned Item',             amount: 45.00,   currency: 'USD', type: 'EXPENSE',  date: ago(5),  categoryId: 124, categoryName: 'Online Shopping', accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CANCELLED',  paymentMethod: 'CREDIT_CARD',   payee: 'Target',             note: 'Cancelled order',             tags: ['Shopping'] },
  { id: 2002, title: 'Lunch at Cafe',             amount: 25.00,   currency: 'USD', type: 'EXPENSE',  date: ago(1),  categoryId: 102, categoryName: 'Dining Out',      accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Cafe Lola',          note: 'Lunch',                       tags: ['Social'],                   latitude: 40.7128, longitude: -74.006 },
  { id: 2003, title: 'New Monitor',               amount: 300.00,  currency: 'USD', type: 'EXPENSE',  date: ago(10), categoryId: 121, categoryName: 'Electronics',     accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Best Buy',           note: 'UltraWide',                   tags: ['Work'] },
  { id: 2005, title: 'Quarterly Dividend',        amount: 250.00,  currency: 'USD', type: 'INCOME',   date: ago(3),  categoryId: 199, categoryName: 'Investment Returns', accountId: 'acc_invest', accountName: 'Investment',         status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Vanguard',           note: 'Q1 Dividends',                tags: ['Investment'] },
  { id: 2006, title: 'Annual Insurance',          amount: 1200.00, currency: 'USD', type: 'EXPENSE',  date: ago(20), categoryId: 182, categoryName: 'Insurance',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Geico',              note: 'Annual auto insurance',       tags: ['Auto','Essential'] },
  { id: 2007, title: 'New Laptop',                amount: 2500.00, currency: 'USD', type: 'EXPENSE',  date: ago(40), categoryId: 121, categoryName: 'Electronics',     accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Apple Store',        note: 'MacBook Pro 16',              tags: ['Work'] },
  { id: 2009, title: 'Bonus 2025',                amount: 3000.00, currency: 'USD', type: 'INCOME',   date: ago(44), categoryId: 197, categoryName: 'Employment',      accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Performance bonus',           tags: ['Work'] },
  { id: 2010, title: 'Charity Donation',          amount: 100.00,  currency: 'USD', type: 'EXPENSE',  date: ago(3),  categoryId: 191, categoryName: 'Donations',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Red Cross',          note: 'Help',                        tags: ['Gifts'] },
  { id: 2011, title: 'Coffee',                    amount: 5.50,    currency: 'USD', type: 'EXPENSE',  date: ago(2),  categoryId: 103, categoryName: 'Coffee & Snacks', accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Local Cafe',         note: 'Morning routine',             tags: ['Food'] },
  { id: 2013, title: 'Weekly Gas',                amount: 60.00,   currency: 'USD', type: 'EXPENSE',  date: ago(1),  categoryId: 110, categoryName: 'Fuel',            accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Shell',              note: 'Gas',                         tags: ['Transport'] },
  { id: 2014, title: 'Uber to Airport',           amount: 45.00,   currency: 'USD', type: 'EXPENSE',  date: ago(0),  categoryId: 112, categoryName: 'Ride & Taxi',     accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Uber',               note: 'Travel day',                  tags: ['Travel'] },
  { id: 2015, title: 'Flight to NYC',             amount: 350.00,  currency: 'USD', type: 'EXPENSE',  date: ago(0),  categoryId: 160, categoryName: 'Flights',         accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Delta',              note: 'Business trip',               tags: ['Travel'] },
  { id: 2016, title: 'Hotel NYC',                 amount: 600.00,  currency: 'USD', type: 'EXPENSE',  date: ago(0),  categoryId: 161, categoryName: 'Accommodation',   accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Hilton',             note: '3 nights',                    tags: ['Travel'],                   latitude: 40.7531, longitude: -73.9822 },
  { id: 2018, title: 'Interest Income',           amount: 12.50,   currency: 'USD', type: 'INCOME',   date: ago(1),  categoryId: 199, categoryName: 'Investment Returns', accountId: 'acc_savings', accountName: 'Savings',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Bank',               note: 'Monthly interest',            tags: ['Savings'] },
  { id: 2020, title: 'Yoga Class',                amount: 20.00,   currency: 'USD', type: 'EXPENSE',  date: ago(3),  categoryId: 142, categoryName: 'Fitness',         accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Yoga Studio',        note: 'Session',                     tags: ['Health'] },
  { id: 2021, title: 'New Shoes',                 amount: 120.00,  currency: 'USD', type: 'EXPENSE',  date: ago(12), categoryId: 120, categoryName: 'Clothing & Apparel', accountId: 'acc_credit', accountName: 'Credit Card',        status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Nike',               note: 'Running shoes',               tags: ['Shopping'] },
  { id: 2022, title: 'Birthday Money',            amount: 200.00,  currency: 'USD', type: 'INCOME',   date: ago(15), categoryId: 200, categoryName: 'Other Income',    accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Family',             note: 'Birthday money',              tags: ['Gifts'] },
  { id: 2025, title: 'Miscellaneous',             amount: 12.00,   currency: 'USD', type: 'EXPENSE',  date: ago(7),  categoryId: 200, categoryName: 'Other Income',    accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Small Shop',         note: 'Misc item',                   tags: ['Misc'] },
  { id: 2026, title: 'Coffee',                    amount: 6.00,    currency: 'USD', type: 'EXPENSE',  date: ago(0),  categoryId: 103, categoryName: 'Coffee & Snacks', accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Starbucks',          note: 'Latte',                       tags: ['Food'] },
  { id: 2027, title: 'Grocery Run',               amount: 85.00,   currency: 'USD', type: 'EXPENSE',  date: ago(0),  categoryId: 101, categoryName: 'Groceries',       accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: 'Safeway',            note: 'Weekly food',                 tags: ['Food'] },
  { id: 2028, title: 'Internet Bill',             amount: 79.99,   currency: 'USD', type: 'EXPENSE',  date: ago(0),  categoryId: 131, categoryName: 'Utilities',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'ISP',                note: 'Monthly bill',                tags: ['Housing'] },
  { id: 2030, title: 'Rent Payment',              amount: 1850.00, currency: 'USD', type: 'EXPENSE',  date: ago(0),  categoryId: 130, categoryName: 'Rent',            accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Landlord',           note: 'Monthly rent',                tags: ['Housing'] },
  { id: 2031, title: 'Salary Deposit',            amount: 5250.00, currency: 'USD', type: 'INCOME',   date: ago(0),  categoryId: 197, categoryName: 'Salary',          accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Monthly salary',              tags: ['Work'] },
  { id: 2032, title: 'Wallet to Bank',            amount: 100.00,  currency: 'USD', type: 'TRANSFER', date: ago(0),  categoryId: null, categoryName: null,             accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: null,                 note: 'Deposit',                     tags: ['Transfer'] },
  { id: 2033, title: 'Wallet to Bank',            amount: 100.00,  currency: 'USD', type: 'TRANSFER', date: ago(0),  categoryId: null, categoryName: null,             accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: null,                 note: 'Deposit',                     tags: ['Transfer'] },
  /* ── future planned transaction (nextMonth sub) ── */
  { id: 2004, title: 'Upcoming Subscription',     amount: 15.00,   currency: 'USD', type: 'EXPENSE',  date: D + 15*DAY, categoryId: 185, categoryName: 'Video Streaming', accountId: 'acc_credit', accountName: 'Credit Card',        status: 'UNCLEARED',  paymentMethod: 'CREDIT_CARD',   payee: 'Hulu',               note: 'Next month',                  tags: ['Entertainment'] },
  /* ── historical fixed-timestamp (Apr-Jul 2025) ── */
  { id: 3001, title: 'Salary Apr 2025',           amount: 5250.00, currency: 'USD', type: 'INCOME',   date: 1743379200000, categoryId: 197, categoryName: 'Salary',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Salary Apr 2025',             tags: ['Work'] },
  { id: 3002, title: 'Rent Apr 2025',             amount: 1850.00, currency: 'USD', type: 'EXPENSE',  date: 1743465600000, categoryId: 130, categoryName: 'Rent',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Landlord',           note: 'Rent Apr 2025',               tags: ['Housing'] },
  { id: 3006, title: 'Dinner Out',                amount: 75.00,   currency: 'USD', type: 'EXPENSE',  date: 1743811200000, categoryId: 102, categoryName: 'Dining Out', accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Restaurant',         note: 'Friday Dinner',               tags: ['Social'] },
  { id: 3011, title: 'Salary May 2025',           amount: 5250.00, currency: 'USD', type: 'INCOME',   date: 1745971200000, categoryId: 197, categoryName: 'Salary',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Salary May 2025',             tags: ['Work'] },
  { id: 3012, title: 'Rent May 2025',             amount: 1850.00, currency: 'USD', type: 'EXPENSE',  date: 1746057600000, categoryId: 130, categoryName: 'Rent',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Landlord',           note: 'Rent May 2025',               tags: ['Housing'] },
  { id: 3014, title: 'New Jacket',                amount: 150.00,  currency: 'USD', type: 'EXPENSE',  date: 1746230400000, categoryId: 120, categoryName: 'Clothing & Apparel', accountId: 'acc_credit', accountName: 'Credit Card',  status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Zara',               note: 'Shopping',                    tags: ['Shopping'] },
  { id: 3019, title: 'Salary Jun 2025',           amount: 5250.00, currency: 'USD', type: 'INCOME',   date: 1748563200000, categoryId: 197, categoryName: 'Salary',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Salary Jun 2025',             tags: ['Work'] },
  { id: 3021, title: 'Summer Vacation Flight',    amount: 850.00,  currency: 'USD', type: 'EXPENSE',  date: 1751241600000, categoryId: 160, categoryName: 'Flights',    accountId: 'acc_credit',  accountName: 'Credit Card',         status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Airline',            note: 'Europe trip',                 tags: ['Travel'] },
  { id: 3022, title: 'Hotel Rome',                amount: 1200.00, currency: 'USD', type: 'EXPENSE',  date: 1751328000000, categoryId: 161, categoryName: 'Accommodation', accountId: 'acc_credit', accountName: 'Credit Card',        status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Hotel',              note: 'Stay in Rome',                tags: ['Travel'],                   latitude: 41.9028, longitude: 12.4964 },
  { id: 3023, title: 'Roman Dinner',              amount: 120.00,  currency: 'EUR', type: 'EXPENSE',  date: 1751414400000, categoryId: 102, categoryName: 'Dining Out', accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'DEBIT_CARD',    payee: 'Trattoria',          note: 'Dinner in Rome',              tags: ['Travel'],                   latitude: 41.8902, longitude: 12.4922 },
  { id: 3026, title: 'Salary Jul 2025',           amount: 5250.00, currency: 'USD', type: 'INCOME',   date: 1751241600000, categoryId: 197, categoryName: 'Salary',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Salary Jul 2025',             tags: ['Work'] },
  { id: 3029, title: 'Bonus Pay',                 amount: 2000.00, currency: 'USD', type: 'INCOME',   date: 1751500800000, categoryId: 197, categoryName: 'Employment', accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Mid-year bonus',              tags: ['Work'] },
  { id: 3058, title: 'Salary Jan 2026',           amount: 5500.00, currency: 'USD', type: 'INCOME',   date: 1767225600000, categoryId: 197, categoryName: 'Salary',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Raise! 2026',                 tags: ['Work'] },
  { id: 3059, title: 'Rent Jan 2026',             amount: 1900.00, currency: 'USD', type: 'EXPENSE',  date: 1767312000000, categoryId: 130, categoryName: 'Rent',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Landlord',           note: 'Rent 2026',                   tags: ['Housing'] },
  { id: 3068, title: 'Tax Refund 2026',           amount: 1500.00, currency: 'USD', type: 'INCOME',   date: 1774828800000, categoryId: 200, categoryName: 'Other Income', accountId: 'acc_bank',  accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'IRS',                note: 'Refund',                      tags: ['Tax'] },
  { id: 3081, title: 'Salary Jul 2026',           amount: 5500.00, currency: 'USD', type: 'INCOME',   date: 1782777600000, categoryId: 197, categoryName: 'Salary',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Salary Jul 2026',             tags: ['Work'] },
  { id: 3082, title: 'Rent Jul 2026',             amount: 1900.00, currency: 'USD', type: 'EXPENSE',  date: 1782864000000, categoryId: 130, categoryName: 'Rent',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'Landlord',           note: 'Rent Jul 2026',               tags: ['Housing'] },
  { id: 3083, title: 'Summer BBQ',                amount: 150.00,  currency: 'USD', type: 'EXPENSE',  date: 1782950400000, categoryId: 101, categoryName: 'Groceries',  accountId: 'acc_cash',    accountName: 'Cash Wallet',         status: 'CLEARED',    paymentMethod: 'CASH',          payee: 'Market',             note: 'Food',                        tags: ['Social'] },
  { id: 3084, title: 'Patio Furniture',           amount: 600.00,  currency: 'USD', type: 'EXPENSE',  date: 1783036800000, categoryId: 122, categoryName: 'Home & Living', accountId: 'acc_credit', accountName: 'Credit Card',        status: 'CLEARED',    paymentMethod: 'CREDIT_CARD',   payee: 'Store',              note: 'Outdoor set',                 tags: ['Shopping'] },
  { id: 3085, title: 'Salary Aug 2026',           amount: 5500.00, currency: 'USD', type: 'INCOME',   date: 1785369600000, categoryId: 197, categoryName: 'Salary',    accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'CLEARED',    paymentMethod: 'BANK_TRANSFER', payee: 'TechCorp Inc',       note: 'Salary Aug 2026',             tags: ['Work'] },
  { id: 3086, title: 'Rent Aug 2026',             amount: 1900.00, currency: 'USD', type: 'EXPENSE',  date: 1785456000000, categoryId: 130, categoryName: 'Rent',       accountId: 'acc_bank',    accountName: 'Main Bank',           status: 'UNCLEARED',  paymentMethod: 'BANK_TRANSFER', payee: 'Landlord',           note: 'Rent Aug 2026 (future)',      tags: ['Housing'] },
];

/* Default transactions endpoint — mock.js will override with filtering */
MOCK_DATA['/api/transactions'] = {
  success: true,
  data: { items: window.MOCK_TRANSACTIONS.slice().sort((a,b)=>b.date-a.date).slice(0,25), total: window.MOCK_TRANSACTIONS.length, page: 0, limit: 25, hasMore: window.MOCK_TRANSACTIONS.length > 25 }
};

/* Per-transaction detail endpoints */
window.MOCK_TRANSACTIONS.forEach(tx => {
  MOCK_DATA[`/api/transactions/${tx.id}`] = { success: true, data: tx };
});

/* Per-account detail endpoints */
MOCK_ACCOUNTS.forEach(acc => {
  const recent = window.MOCK_TRANSACTIONS
    .filter(t => t.accountId === acc.id)
    .sort((a,b) => b.date - a.date)
    .slice(0, 8);
  MOCK_DATA[`/api/accounts/${acc.id}`] = {
    success: true,
    data: { account: acc, recentTransactions: recent }
  };
});

/* ────────────── SAVED FILTERS ────────────── */
MOCK_DATA['/api/filters'] = {
  success: true,
  data: [
    { id: 'f-1', name: 'Food & Drink Expense',  categoryIds: [102, 101, 103], transactionTypes: ['EXPENSE'], currencies: [],        tags: [],            queryPayee: '' },
    { id: 'f-2', name: 'Shopping Expense',       categoryIds: [120, 121, 124], transactionTypes: ['EXPENSE'], currencies: [],        tags: [],            queryPayee: '' },
    { id: 'f-3', name: 'Salary Income',          categoryIds: [197],           transactionTypes: ['INCOME'],  currencies: [],        tags: [],            queryPayee: '' },
    { id: 'f-4', name: 'Transport Expense',      categoryIds: [110, 111, 112], transactionTypes: ['EXPENSE'], currencies: [],        tags: [],            queryPayee: '' },
    { id: 'f-5', name: 'Entertainment Expense',  categoryIds: [150, 151, 152], transactionTypes: ['EXPENSE'], currencies: [],        tags: [],            queryPayee: '' },
    { id: 'f-6', name: 'Food Budget Filter',     categoryIds: [101, 102, 103], transactionTypes: ['EXPENSE'], currencies: [],        tags: [],            queryPayee: '' },
    { id: 'f-7', name: 'Japan Trip Filter',      categoryIds: [],              transactionTypes: [],          currencies: ['JPY'],   tags: [],            queryPayee: '' },
    { id: 'f-8', name: 'Car Loan Filter',        categoryIds: [],              transactionTypes: [],          currencies: [],        tags: ['CarLoan'],   queryPayee: '' },
    { id: 'f-9', name: 'Friend Loan Filter',     categoryIds: [],              transactionTypes: [],          currencies: [],        tags: [],            queryPayee: 'John Doe' },
  ]
};

/* ────────────── BUDGETS ────────────── */
const MOCK_BUDGETS = [
  { id: 'b-1', name: 'Monthly Food',  currency: 'USD', limitAmount: 800.00,    spent: 380.20, remaining: 419.80,  percentage: 47.5, alertLevel: 'NONE',    alertThreshold: 0.8, timelineType: 'MONTHLY', isArchived: false, categoryIds: [101,102,103], accountIds: [] },
  { id: 'b-2', name: 'Japan Trip',    currency: 'JPY', limitAmount: 200000.00, spent: 8500.00,remaining: 191500.00,percentage: 4.3,  alertLevel: 'NONE',    alertThreshold: 0.8, timelineType: 'ALL_TIME', isArchived: false, categoryIds: [],            accountIds: ['acc_jpy'] },
];

MOCK_DATA['/api/budgets']                         = { success: true, data: MOCK_BUDGETS };
MOCK_DATA['/api/budgets?includeArchived=true']    = { success: true, data: MOCK_BUDGETS };
MOCK_BUDGETS.forEach(b => { MOCK_DATA[`/api/budgets/${b.id}`] = { success: true, data: b }; });

/* ────────────── LOANS ────────────── */
const MOCK_LOANS = [
  { id: 'l-1', name: 'Car Loan',     loanType: 'BORROWED', lenderOrBorrowerName: 'Chase Auto', currency: 'USD', principalAmount: 25000.00, totalPaid: 4500.00,  remainingBalance: 20500.00, status: 'ACTIVE',  startDate: ago(100), expectedSettlementDate: D + DAY*900, interestRate: 3.5, interestAccrued: 875.00, linkedAccountId: 'acc_bank' },
  { id: 'l-2', name: 'Lent to John', loanType: 'LENT',     lenderOrBorrowerName: 'John Doe',   currency: 'USD', principalAmount: 500.00,   totalPaid: 100.00,   remainingBalance: 400.00,   status: 'ACTIVE',  startDate: ago(10),  expectedSettlementDate: D + DAY*60,  interestRate: 0,   interestAccrued: 0,      linkedAccountId: 'acc_cash' },
];

MOCK_DATA['/api/loans'] = { success: true, data: MOCK_LOANS };
MOCK_LOANS.forEach(l => {
  MOCK_DATA[`/api/loans/${l.id}`] = { success: true, data: l };
  MOCK_DATA[`/api/loans/${l.id}/payments`] = { success: true, data:
    l.id === 'l-1' ? [
      { id: 'lp-1', amount: 450.00, currency: 'USD', date: ago(10),  note: 'Monthly installment' },
      { id: 'lp-2', amount: 450.00, currency: 'USD', date: ago(40),  note: 'Monthly installment' },
      { id: 'lp-3', amount: 450.00, currency: 'USD', date: ago(70),  note: 'Monthly installment' },
      { id: 'lp-4', amount: 450.00, currency: 'USD', date: ago(100), note: 'Monthly installment' },
    ] : [
      { id: 'lp-5', amount: 100.00, currency: 'USD', date: ago(2), note: 'Partial repayment' },
    ]
  };
});

/* ────────────── PLANNED PAYMENTS ────────────── */
const NOW = Date.now();
const MOCK_PLANNED = [
  { id: 'pp-1', name: 'Monthly Rent',    amount: 1850.00, currency: 'USD', transactionType: 'EXPENSE', recurrenceDescription: 'Every month on day 1', nextDueDate: NOW + DAY*13, categoryId: 130, categoryName: 'Rent',            accountId: 'acc_bank',    accountName: 'Main Bank',   status: 'ACTIVE', isOverdue: false, autoConfirm: true,  payee: 'Property Management', note: 'Monthly rent payment', tags: ['Housing','Essential'] },
  { id: 'pp-2', name: 'Salary Deposit',  amount: 5250.00, currency: 'USD', transactionType: 'INCOME',  recurrenceDescription: 'Every month on day 1', nextDueDate: NOW + DAY*13, categoryId: 197, categoryName: 'Employment',      accountId: 'acc_bank',    accountName: 'Main Bank',   status: 'ACTIVE', isOverdue: false, autoConfirm: false, payee: 'TechCorp Inc',        note: 'Monthly salary',       tags: ['Work'] },
  { id: 'pp-3', name: 'Quick Coffee',    amount: 6.50,    currency: 'USD', transactionType: 'EXPENSE',  recurrenceDescription: 'Every week on Tuesday', nextDueDate: NOW + DAY*3,  categoryId: 103, categoryName: 'Coffee & Snacks', accountId: 'acc_cash',    accountName: 'Cash Wallet', status: 'ACTIVE', isOverdue: false, autoConfirm: false, payee: 'Starbucks',           note: 'Latte',                tags: ['Social'] },
];

MOCK_DATA['/api/planned-payments']         = { success: true, data: MOCK_PLANNED };
MOCK_DATA['/api/planned-payments/upcoming']= { success: true, data: MOCK_PLANNED.filter(p => !p.isOverdue && p.status === 'ACTIVE') };
MOCK_DATA['/api/planned-payments/overdue'] = { success: true, data: MOCK_PLANNED.filter(p => p.isOverdue) };
MOCK_PLANNED.forEach(p => {
  MOCK_DATA[`/api/planned-payments/${p.id}`] = { success: true, data: p };
  MOCK_DATA[`/api/planned-payments/${p.id}/occurrences`] = { success: true, data: [] };
});

/* ────────────── TEMPLATES ────────────── */
const MOCK_TEMPLATES = [
  { id: 't-1', name: 'Monthly Rent',    amount: 1850.00, currency: 'USD', transactionType: 'EXPENSE', categoryId: 130, categoryName: 'Rent',            accountId: 'acc_bank',    accountName: 'Main Bank',   tags: ['Housing','Essential'], payee: 'Property Management', note: 'Monthly rent payment', paymentMethod: 'BANK_TRANSFER', transactionStatus: 'CLEARED', isStarred: true },
  { id: 't-2', name: 'Quick Coffee',    amount: 6.50,    currency: 'USD', transactionType: 'EXPENSE', categoryId: 103, categoryName: 'Coffee & Snacks', accountId: 'acc_cash',    accountName: 'Cash Wallet', tags: ['Social'],              payee: 'Starbucks',           note: 'Latte',                paymentMethod: 'CASH',          transactionStatus: 'CLEARED', isStarred: false },
  { id: 't-3', name: 'Salary Deposit',  amount: 5250.00, currency: 'USD', transactionType: 'INCOME',  categoryId: 197, categoryName: 'Employment',      accountId: 'acc_bank',    accountName: 'Main Bank',   tags: ['Work'],                payee: 'TechCorp Inc',        note: 'Monthly salary',       paymentMethod: 'BANK_TRANSFER', transactionStatus: 'CLEARED', isStarred: true },
  { id: 't-4', name: 'Gojek Bali',      amount: 35000.00,currency: 'IDR', transactionType: 'EXPENSE', categoryId: 112, categoryName: 'Ride & Taxi',     accountId: 'acc_idr',     accountName: 'GoPay IDR',   tags: ['Travel'],              payee: 'Gojek',               note: 'Ride in Bali',         paymentMethod: 'DIGITAL_WALLET',transactionStatus: 'CLEARED', isStarred: false },
];

MOCK_DATA['/api/templates'] = { success: true, data: MOCK_TEMPLATES };
MOCK_TEMPLATES.forEach(t => { MOCK_DATA[`/api/templates/${t.id}`] = { success: true, data: t }; });

/* ────────────── DASHBOARD CARDS ────────────── */
const mkFilter = (tl = 'MONTH') => ({
  timelineType: tl, dateFrom: null, dateTo: null,
  accountIds: [], categoryIds: [], transactionTypes: [], tags: [],
  currencies: [], statuses: [], paymentMethods: [], queryNote: '',
  queryPayee: '', minAmount: null, maxAmount: null, excludeFuture: true,
});

const L6M   = ['Feb','Mar','Apr','May','Jun','Jul'];
const mkBkt = (l,i,e) => ({ label:l, income:5250+i*50, expense:e?-(e+i*30):-(3200+i*80), net:(5250+i*50)-(e||3200)-i*30 });

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
      type: 'NetWorth', netWorth: 46855.00, income: 5250.00, expense: -4005.00, transfer: 0,
      previousNetWorth: 43500.00, previousIncome: 5250.00, previousExpense: -3850.00, previousTransfer: 0,
      currency: 'USD', fontSize: 'Medium', perspective: 'Accounting', showComparison: true,
    }
  },
  {
    cardId: 'dc-cashflow', cardType: 'CASH_FLOW', title: 'Cash Flow',
    order: 2, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { perspective: 'Accounting', showNetColumn: true, showBaseConversion: true },
    data: {
      type: 'CashFlow', currency: 'USD', perspective: 'Accounting', showNetColumn: true, showBaseConversion: true,
      income: 5250.00, expense: -4005.00, transfer: -100.00, net: 1145.00,
      breakdown: [
        { currency: 'USD', income: 5250, incomeInBase: 5250, expense: -4005, expenseInBase: -4005, transfer: -100, transferInBase: -100, transferIn: 100, transferOut: 200, transferInInBase: 100, transferOutInBase: 200, net: 1145 },
        { currency: 'JPY', income: 0,    incomeInBase: 0,    expense: -8500, expenseInBase: -58,   transfer: 29400, transferInBase: 200, transferIn: 29400, transferOut: 0, transferInInBase: 200, transferOutInBase: 0, net: 142 },
        { currency: 'EUR', income: 1500, incomeInBase: 1620, expense: -165,  expenseInBase: -178,  transfer: 0,     transferInBase: 0,   transferIn: 0, transferOut: 0, transferInInBase: 0, transferOutInBase: 0, net: 1442 },
      ]
    }
  },
  {
    cardId: 'dc-recent', cardType: 'RECENT_ACTIVITIES', title: 'Recent Activities',
    order: 3, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('WEEK'), config: { showCategory: true, showAccount: true, showNote: false, showTags: true },
    data: {
      type: 'RecentActivities', showCategory: true, showAccount: true, showNote: false, showTags: true,
      transactions: window.MOCK_TRANSACTIONS.slice().sort((a,b)=>b.date-a.date).slice(0,8).map(tx => ({ ...tx, isPlanned: false }))
    }
  },
  {
    cardId: 'dc-currency', cardType: 'CURRENCY_BALANCES', title: 'Currency Balances',
    order: 4, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('ALL_TIME'), config: { layout: 'grid', gridColumns: 2, showBaseConversion: true },
    data: {
      type: 'CurrencyBalances', baseCurrency: 'USD', layout: 'grid', gridColumns: 2, showBaseConversion: true,
      items: [
        { currency: 'USD', balance: 31085.00, balanceInBase: 31085.00 },
        { currency: 'JPY', balance: 115400.00, balanceInBase: 785.71 },
        { currency: 'SGD', balance: 5400.00, balanceInBase: 4060.15 },
        { currency: 'IDR', balance: 9220000.00, balanceInBase: 591.03 },
        { currency: 'INR', balance: 120000.00, balanceInBase: 1439.23 },
        { currency: 'EUR', balance: 1500.00, balanceInBase: 1620.00 },
      ]
    }
  },
  {
    cardId: 'dc-budgets', cardType: 'BUDGETS', title: 'Budgets',
    order: 5, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: {},
    data: { type: 'Budgets', currency: 'USD', items: MOCK_BUDGETS.filter(b => !b.isArchived) }
  },
  {
    cardId: 'dc-planned', cardType: 'PLANNED_PAYMENTS', title: 'Planned Payments',
    order: 6, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: {},
    data: {
      type: 'PlannedPayments', currency: 'USD',
      upcoming: MOCK_PLANNED.filter(p => !p.isOverdue && p.status === 'ACTIVE'),
      overdue: MOCK_PLANNED.filter(p => p.isOverdue)
    }
  },
  {
    cardId: 'dc-loans', cardType: 'LOANS', title: 'Loans',
    order: 7, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('ALL_TIME'), config: {},
    data: { type: 'Loans', currency: 'USD', items: MOCK_LOANS }
  },
  {
    cardId: 'dc-hero', cardType: 'ACCOUNT_HERO', title: 'Account Hero',
    order: 8, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { perspective: 'Accounting', showCurrencyBadges: true },
    data: {
      type: 'AccountHero', currency: 'USD', colorHex: '#2196F3',
      balance: 8420.00, balanceInBase: 8420.00,
      income: 5250.00, expense: -4005.00, transferIn: 550.00, transferOut: 3550.00,
      perspective: 'Accounting', showCurrencyBadges: true,
      distribution: { USD: 7500.00, INR: 120000.00, EUR: 1500.00 },
    }
  },
  {
    cardId: 'dc-catpie', cardType: 'CATEGORY_PIE_CHART', title: 'Category Breakdown',
    order: 9, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { perspective: 'Accounting' },
    data: {
      type: 'CategoryBreakdown', displayCurrency: 'USD',
      items: [
        { categoryId: 130, categoryName: 'Rent',            amount: -1850.00, percentage: 0.462, colorHex: C.housing,       subcategories: [] },
        { categoryId: 197, categoryName: 'Employment',      amount: 5250.00,  percentage: 0.0,   colorHex: C.income,        subcategories: [] },
        { categoryId: 131, categoryName: 'Utilities',       amount: -284.99,  percentage: 0.071, colorHex: C.housing,       subcategories: [] },
        { categoryId: 102, categoryName: 'Dining Out',      amount: -315.50,  percentage: 0.079, colorHex: C.food,          subcategories: [] },
        { categoryId: 101, categoryName: 'Groceries',       amount: -362.70,  percentage: 0.091, colorHex: C.food,          subcategories: [] },
        { categoryId: 110, categoryName: 'Fuel',            amount: -115.00,  percentage: 0.029, colorHex: C.transport,     subcategories: [] },
        { categoryId: 185, categoryName: 'Video Streaming', amount: -15.99,   percentage: 0.004, colorHex: C.subscriptions, subcategories: [] },
        { categoryId: 142, categoryName: 'Fitness',         amount: -49.99,   percentage: 0.012, colorHex: C.health,        subcategories: [] },
        { categoryId: 141, categoryName: 'Pharmacy',        amount: -35.00,   percentage: 0.009, colorHex: C.health,        subcategories: [] },
        { categoryId: 161, categoryName: 'Accommodation',   amount: -220.00,  percentage: 0.055, colorHex: C.travel,        subcategories: [] },
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
        { categoryId: 130, categoryName: 'Rent',            amount: -1850.00, percentage: 46.2, colorHex: C.housing,       subcategories: [] },
        { categoryId: 101, categoryName: 'Groceries',       amount: -362.70,  percentage: 9.1,  colorHex: C.food,          subcategories: [] },
        { categoryId: 102, categoryName: 'Dining Out',      amount: -315.50,  percentage: 7.9,  colorHex: C.food,          subcategories: [] },
        { categoryId: 131, categoryName: 'Utilities',       amount: -284.99,  percentage: 7.1,  colorHex: C.housing,       subcategories: [] },
        { categoryId: 161, categoryName: 'Accommodation',   amount: -220.00,  percentage: 5.5,  colorHex: C.travel,        subcategories: [] },
        { categoryId: 110, categoryName: 'Fuel',            amount: -115.00,  percentage: 2.9,  colorHex: C.transport,     subcategories: [] },
        { categoryId: 142, categoryName: 'Fitness',         amount: -49.99,   percentage: 1.2,  colorHex: C.health,        subcategories: [] },
        { categoryId: 141, categoryName: 'Pharmacy',        amount: -35.00,   percentage: 0.9,  colorHex: C.health,        subcategories: [] },
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
      current:  L6M.map((l,i) => mkBkt(l,i,3200)),
      previous: L6M.map((l,i) => mkBkt(l,i,3100)),
      lastYear: L6M.map((l,i) => mkBkt(l,i,2800)),
    }
  },
  {
    cardId: 'dc-iebar', cardType: 'INCOME_EXPENSE_BAR', title: 'Income & Expense',
    order: 12, isPinned: false, isEnabled: true,
    filterSummary: mkFilter('MONTH'), config: { isCumulative: false },
    data: {
      type: 'IncomeExpenseBar', baseCurrency: 'USD', granularity: 'WEEK', isCumulative: false, todayBucketIndex: 3,
      buckets: [
        { label: 'W1', income: 0,      expense: -1105 },
        { label: 'W2', income: 5250,   expense: -1380 },
        { label: 'W3', income: 1200,   expense: -920  },
        { label: 'W4', income: 0,      expense: -600  },
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
        { categoryId: 130, categoryName: 'Rent',      amount: -1850, colorHex: C.housing, nature: 'MUST' },
        { categoryId: 101, categoryName: 'Groceries', amount: -362,  colorHex: C.food,    nature: 'NEED' },
        { categoryId: 102, categoryName: 'Dining Out',amount: -315,  colorHex: C.food,    nature: 'WANT' },
      ],
      buckets: L6M.map((l, i) => ({
        label: l,
        categories: [
          { nature: 'MUST', amount: 1850+i*0 },
          { nature: 'NEED', amount: 320+i*15 },
          { nature: 'WANT', amount: 280+i*20 },
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
        { tagName: 'Housing',   income: 0,      expense: -2284.99, transactionCount: 4 },
        { tagName: 'Work',      income: 5250,   expense: -54.99,   transactionCount: 5 },
        { tagName: 'Food',      income: 0,      expense: -543.70,  transactionCount: 7 },
        { tagName: 'Travel',    income: 0,      expense: -509.00,  transactionCount: 4 },
        { tagName: 'Essential', income: 0,      expense: -2284.99, transactionCount: 4 },
        { tagName: 'Social',    income: 0,      expense: -315.50,  transactionCount: 5 },
        { tagName: 'Transport', income: 0,      expense: -115.00,  transactionCount: 2 },
      ],
      intersections: [
        { tags: ['Housing','Essential'], income: 0,    expense: -2284.99, transactionCount: 4 },
        { tags: ['Food','Social'],       income: 0,    expense: -100.50,  transactionCount: 2 },
        { tags: ['Travel','Transport'],  income: 0,    expense: -69.00,   transactionCount: 2 },
      ]
    }
  },
];

MOCK_DATA['/api/dashboard'] = { success: true, data: DASH_CARDS };

DASH_CARDS.forEach(c => {
  MOCK_DATA[`/api/dashboard/cards/${c.cardId}`] = { success: true, data: c };
});

/* ────────────── ANALYTICS ────────────── */
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

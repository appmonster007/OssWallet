-- ============================================================
-- Root Categories
-- Colors are Material Design 700-900 shades, one per domain.
-- colorHex = -16777216 + R*65536 + G*256 + B  (ARGB signed int)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1,  'Food & Drink',    'Restaurant',      -689152,   1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (2,  'Transport',       'DirectionsCar',   -15374912, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (3,  'Shopping',        'ShoppingBag',     -8708190,  1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (4,  'Housing',         'Home',            -16750244, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (5,  'Health',          'HealthAndSafety', -3790808,  1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (6,  'Entertainment',   'Movie',           -9623334,  1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (7,  'Travel',          'Flight',          -16615491, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (8,  'Education',       'School',          -15064194, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (9,  'Finance',         'AccountBalance',  -13407970, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (10, 'Subscriptions',   'Subscriptions',   -13558894, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (11, 'Gifts & Charity', 'CardGiftcard',    -7860657,  1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (12, 'Business',        'Work',            -13154481, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (13, 'Income',          'Payments',        -14983648, 1, NULL)

-- ============================================================
-- System Categories  (mandatory, reserved IDs — never delete)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1000, 'Others',   'Menu',          -7829368, 1, NULL)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1001, 'Missing',  'MoreHoriz',     -7829368, 1, 1000)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1002, 'Unknown',  'QuestionMark',  -7829368, 1, 1000)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1003, 'Transfer', 'SwapHoriz',     -7829368, 1, 1000)

-- ============================================================
-- Food & Drink — Level 1  (parentId = 1)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (101, 'Groceries',       'LocalGroceryStore', -689152, 1, 1)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (102, 'Dining Out',      'LocalDining',       -689152, 1, 1)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (103, 'Coffee & Snacks', 'Coffee',            -689152, 1, 1)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (104, 'Bars & Nightlife','LocalBar',          -689152, 1, 1)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (105, 'Food Delivery',   'DeliveryDining',    -689152, 1, 1)

-- Food & Drink — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Supermarket',    'Store',         -689152, 1, 101)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Specialty Food', 'Storefront',    -689152, 1, 101)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Restaurants',    'Restaurant',    -689152, 1, 102)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Fast Food',      'Fastfood',      -689152, 1, 102)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Alcohol',        'LocalBar',      -689152, 1, 104)

-- ============================================================
-- Transport — Level 1  (parentId = 2)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (110, 'Own Vehicle',    'DirectionsCar', -15374912, 1, 2)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (111, 'Public Transit', 'DirectionsBus', -15374912, 1, 2)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (112, 'Ride & Taxi',    'LocalTaxi',     -15374912, 1, 2)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (113, 'Cycling',        'PedalBike',     -15374912, 1, 2)

-- Transport — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Fuel',            'LocalGasStation', -15374912, 1, 110)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Parking',         'LocalParking',    -15374912, 1, 110)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Car Maintenance', 'Build',           -15374912, 1, 110)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Car Wash',        'LocalCarWash',    -15374912, 1, 110)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Bus & Metro',     'DirectionsBus',   -15374912, 1, 111)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Train',           'Train',           -15374912, 1, 111)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Rideshare',       'Hail',            -15374912, 1, 112)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Taxi',            'LocalTaxi',       -15374912, 1, 112)

-- ============================================================
-- Shopping — Level 1  (parentId = 3)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (120, 'Clothing & Apparel', 'Checkroom',  -8708190, 1, 3)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (121, 'Electronics',        'Devices',    -8708190, 1, 3)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (122, 'Home & Living',      'Chair',      -8708190, 1, 3)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (123, 'Books & Stationery', 'MenuBook',   -8708190, 1, 3)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (124, 'Online Shopping',    'ShoppingCart', -8708190, 1, 3)

-- Shopping — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Clothing',        'Checkroom',     -8708190, 1, 120)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Shoes',           'Hiking',        -8708190, 1, 120)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Accessories',     'WatchLater',    -8708190, 1, 120)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Phones',          'Smartphone',    -8708190, 1, 121)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Computers',       'LaptopMac',     -8708190, 1, 121)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Furniture',       'Chair',         -8708190, 1, 122)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Appliances',      'Kitchen',       -8708190, 1, 122)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Home & Garden',   'LocalFlorist',  -8708190, 1, 122)

-- ============================================================
-- Housing — Level 1  (parentId = 4)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (130, 'Rent',          'Key',               -16750244, 1, 4)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (131, 'Utilities',     'Bolt',              -16750244, 1, 4)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (132, 'Home Services', 'HomeRepairService', -16750244, 1, 4)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (133, 'Furnishing',    'Bed',               -16750244, 1, 4)

-- Housing — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Mortgage',     'Home',                -16750244, 1, 130)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Electricity',  'Lightbulb',           -16750244, 1, 131)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Water',        'WaterDrop',           -16750244, 1, 131)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gas',          'LocalFireDepartment', -16750244, 1, 131)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Internet',     'Wifi',                -16750244, 1, 131)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Phone',        'Phone',               -16750244, 1, 131)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Cleaning',     'CleaningServices',    -16750244, 1, 132)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Repairs',      'Build',               -16750244, 1, 132)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Security',     'Security',            -16750244, 1, 132)

-- ============================================================
-- Health — Level 1  (parentId = 5)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (140, 'Medical',           'MedicalServices',  -3790808, 1, 5)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (141, 'Pharmacy',          'LocalPharmacy',    -3790808, 1, 5)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (142, 'Fitness',           'FitnessCenter',    -3790808, 1, 5)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (143, 'Mental & Wellbeing','SelfImprovement',  -3790808, 1, 5)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (144, 'Personal Care',     'Spa',              -3790808, 1, 5)

-- Health — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Doctor',          'LocalHospital',   -3790808, 1, 140)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Dentist',         'MedicalServices', -3790808, 1, 140)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Hospital',        'LocalHospital',   -3790808, 1, 140)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Vision',          'Visibility',      -3790808, 1, 140)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gym',             'FitnessCenter',   -3790808, 1, 142)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Sports',          'Sports',          -3790808, 1, 142)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Fitness Classes', 'DirectionsRun',   -3790808, 1, 142)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Haircut',         'ContentCut',      -3790808, 1, 144)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Beauty',          'Face',            -3790808, 1, 144)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Spa & Massage',   'Spa',             -3790808, 1, 144)

-- ============================================================
-- Entertainment — Level 1  (parentId = 6)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (150, 'Movies & Theater', 'Movie',         -9623334, 1, 6)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (151, 'Music',            'MusicNote',     -9623334, 1, 6)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (152, 'Gaming',           'SportsEsports', -9623334, 1, 6)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (153, 'Events & Shows',   'EventNote',     -9623334, 1, 6)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (154, 'Hobbies',          'Palette',       -9623334, 1, 6)

-- Entertainment — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Cinema',        'Theaters',       -9623334, 1, 150)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Streaming',     'PlayArrow',      -9623334, 1, 150)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Concerts',      'LibraryMusic',   -9623334, 1, 151)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Live Shows',    'TheaterComedy',  -9623334, 1, 153)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Sports Events', 'Stadium',        -9623334, 1, 153)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Arts & Crafts', 'Brush',          -9623334, 1, 154)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Photography',   'CameraAlt',      -9623334, 1, 154)

-- ============================================================
-- Travel — Level 1  (parentId = 7)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (160, 'Flights',          'Flight',        -16615491, 1, 7)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (161, 'Accommodation',    'Hotel',         -16615491, 1, 7)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (162, 'Ground Transport', 'DirectionsBus', -16615491, 1, 7)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (163, 'Activities',       'Attractions',   -16615491, 1, 7)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (164, 'Travel Essentials','Luggage',       -16615491, 1, 7)

-- Travel — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Hotels',           'Hotel',          -16615491, 1, 161)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Hostels',          'Bed',            -16615491, 1, 161)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Vacation Rental',  'Home',           -16615491, 1, 161)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Car Rental',       'DirectionsCar',  -16615491, 1, 162)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Travel Insurance', 'Shield',         -16615491, 1, 164)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Visas & Permits',  'CardMembership', -16615491, 1, 164)

-- ============================================================
-- Education — Level 1  (parentId = 8)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (170, 'Tuition & Fees',  'School',        -15064194, 1, 8)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (171, 'Online Learning', 'OndemandVideo', -15064194, 1, 8)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (172, 'Books & Supplies','AutoStories',   -15064194, 1, 8)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (173, 'Childcare',       'ChildCare',     -15064194, 1, 8)

-- Education — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('University',     'AccountBalance', -15064194, 1, 170)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Training',       'Class',          -15064194, 1, 170)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Certifications', 'Verified',       -15064194, 1, 170)

-- ============================================================
-- Finance — Level 1  (parentId = 9)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (180, 'Savings',         'Savings',       -13407970, 1, 9)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (181, 'Investments',     'TrendingUp',    -13407970, 1, 9)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (182, 'Insurance',       'Shield',        -13407970, 1, 9)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (183, 'Taxes & Fees',    'Receipt',       -13407970, 1, 9)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (184, 'Loans & Debt',    'Money',         -13407970, 1, 9)

-- Finance — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Stocks',            'ShowChart',        -13407970, 1, 181)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Mutual Funds',      'BarChart',         -13407970, 1, 181)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Crypto',            'CurrencyBitcoin',  -13407970, 1, 181)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Real Estate',       'Domain',           -13407970, 1, 181)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Bonds & Funds',     'AccountBalance',   -13407970, 1, 181)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Life Insurance',    'Favorite',         -13407970, 1, 182)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Vehicle Insurance', 'DirectionsCar',    -13407970, 1, 182)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Property Insurance','Home',             -13407970, 1, 182)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Student Loan',      'School',           -13407970, 1, 184)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Credit Card Debt',  'CreditCard',       -13407970, 1, 184)

-- ============================================================
-- Subscriptions — Level 1  (parentId = 10)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (185, 'Video Streaming',       'PlayArrow',      -13558894, 1, 10)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (186, 'Music & Audio',         'MusicNote',      -13558894, 1, 10)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (187, 'Software Subscriptions','Laptop',         -13558894, 1, 10)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (188, 'News & Reading',        'Article',        -13558894, 1, 10)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (189, 'Memberships',           'CardMembership', -13558894, 1, 10)

-- Subscriptions — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Cloud Storage', 'Cloud',     -13558894, 1, 187)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gaming Pass',   'Gamepad',   -13558894, 1, 187)

-- ============================================================
-- Gifts & Charity — Level 1  (parentId = 11)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (190, 'Gifts',          'CardGiftcard',      -7860657, 1, 11)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (191, 'Donations',      'VolunteerActivism', -7860657, 1, 11)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (192, 'Family Support', 'FamilyRestroom',    -7860657, 1, 11)

-- ============================================================
-- Business — Level 1  (parentId = 12)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (193, 'Office & Supplies',     'BusinessCenter', -13154481, 1, 12)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (194, 'Marketing',             'Campaign',       -13154481, 1, 12)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (195, 'Professional Services', 'Gavel',          -13154481, 1, 12)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (196, 'Client Entertainment',  'Groups',         -13154481, 1, 12)

-- ============================================================
-- Income — Level 1  (parentId = 13)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (197, 'Employment',         'Work',       -14983648, 1, 13)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (198, 'Freelance',          'Handshake',  -14983648, 1, 13)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (199, 'Investment Returns', 'TrendingUp', -14983648, 1, 13)
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (200, 'Other Income',       'Payments',   -14983648, 1, 13)

-- Income — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Salary',              'Payments',       -14983648, 1, 197)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Bonus',               'Star',           -14983648, 1, 197)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Commission',          'Percent',        -14983648, 1, 197)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Dividends',           'AccountBalance', -14983648, 1, 199)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Capital Gains',       'TrendingUp',     -14983648, 1, 199)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Rental Income',       'Home',           -14983648, 1, 199)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Refunds & Cashback',  'ReceiptLong',    -14983648, 1, 200)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Tax Refund',          'AccountBalance', -14983648, 1, 200)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Government Benefits', 'AccountBalance', -14983648, 1, 200)
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gifts Received',      'CardGiftcard',   -14983648, 1, 200)

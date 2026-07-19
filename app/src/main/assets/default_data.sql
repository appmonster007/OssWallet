-- ============================================================
-- Root Categories
-- Colors are Material Design 700-900 shades, one per domain.
-- colorHex = -16777216 + R*65536 + G*256 + B  (ARGB signed int)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1,  'Food & Drink',    'Restaurant',      -689152,   1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (2,  'Transport',       'DirectionsCar',   -15374912, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (3,  'Shopping',        'ShoppingBag',     -8708190,  1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (4,  'Housing',         'Home',            -16750244, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (5,  'Health',          'HealthAndSafety', -3790808,  1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (6,  'Entertainment',   'Movie',           -9623334,  1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (7,  'Travel',          'Flight',          -16615491, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (8,  'Education',       'School',          -15064194, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (9,  'Finance',         'AccountBalance',  -13407970, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (10, 'Subscriptions',   'Subscriptions',   -13558894, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (11, 'Gifts & Charity', 'CardGiftcard',    -7860657,  1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (12, 'Business',        'Work',            -13154481, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (13, 'Income',          'Payments',        -14983648, 1, NULL, 'need')

-- ============================================================
-- System Categories  (mandatory, reserved IDs — never delete)
-- ============================================================


-- ============================================================
-- Food & Drink — Level 1  (parentId = 1)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (101, 'Groceries',       'LocalGroceryStore', -689152, 1, 1, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (102, 'Dining Out',      'LocalDining',       -689152, 1, 1, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (103, 'Coffee & Snacks', 'Coffee',            -689152, 1, 1, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (104, 'Bars & Nightlife','LocalBar',          -689152, 1, 1, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (105, 'Food Delivery',   'DeliveryDining',    -689152, 1, 1, 'want')

-- Food & Drink — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Supermarket',    'Store',         -689152, 1, 101, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Specialty Food', 'Storefront',    -689152, 1, 101, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Restaurants',    'Restaurant',    -689152, 1, 102, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fast Food',      'Fastfood',      -689152, 1, 102, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Alcohol',        'LocalBar',      -689152, 1, 104, 'want')

-- ============================================================
-- Transport — Level 1  (parentId = 2)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (110, 'Own Vehicle',    'DirectionsCar', -15374912, 1, 2, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (111, 'Public Transit', 'DirectionsBus', -15374912, 1, 2, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (112, 'Ride & Taxi',    'LocalTaxi',     -15374912, 1, 2, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (113, 'Cycling',        'PedalBike',     -15374912, 1, 2, 'want')

-- Transport — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fuel',            'LocalGasStation', -15374912, 1, 110, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Parking',         'LocalParking',    -15374912, 1, 110, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Maintenance', 'Build',           -15374912, 1, 110, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Wash',        'LocalCarWash',    -15374912, 1, 110, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bus & Metro',     'DirectionsBus',   -15374912, 1, 111, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Train',           'Train',           -15374912, 1, 111, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Rideshare',       'Hail',            -15374912, 1, 112, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Taxi',            'LocalTaxi',       -15374912, 1, 112, 'want')

-- ============================================================
-- Shopping — Level 1  (parentId = 3)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (120, 'Clothing & Apparel', 'Checkroom',  -8708190, 1, 3, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (121, 'Electronics',        'Devices',    -8708190, 1, 3, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (122, 'Home & Living',      'Chair',      -8708190, 1, 3, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (123, 'Books & Stationery', 'MenuBook',   -8708190, 1, 3, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (124, 'Online Shopping',    'ShoppingCart', -8708190, 1, 3, 'want')

-- Shopping — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Clothing',        'Checkroom',     -8708190, 1, 120, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Shoes',           'Hiking',        -8708190, 1, 120, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Accessories',     'WatchLater',    -8708190, 1, 120, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Phones',          'Smartphone',    -8708190, 1, 121, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Computers',       'LaptopMac',     -8708190, 1, 121, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Furniture',       'Chair',         -8708190, 1, 122, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Appliances',      'Kitchen',       -8708190, 1, 122, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Home & Garden',   'LocalFlorist',  -8708190, 1, 122, 'want')

-- ============================================================
-- Housing — Level 1  (parentId = 4)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (130, 'Rent',          'Key',               -16750244, 1, 4, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (131, 'Utilities',     'Bolt',              -16750244, 1, 4, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (132, 'Home Services', 'HomeRepairService', -16750244, 1, 4, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (133, 'Furnishing',    'Bed',               -16750244, 1, 4, 'want')

-- Housing — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Mortgage',     'Home',                -16750244, 1, 130, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Electricity',  'Lightbulb',           -16750244, 1, 131, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Water',        'WaterDrop',           -16750244, 1, 131, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gas',          'LocalFireDepartment', -16750244, 1, 131, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Internet',     'Wifi',                -16750244, 1, 131, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Phone',        'Phone',               -16750244, 1, 131, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cleaning',     'CleaningServices',    -16750244, 1, 132, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Repairs',      'Build',               -16750244, 1, 132, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Security',     'Security',            -16750244, 1, 132, 'need')

-- ============================================================
-- Health — Level 1  (parentId = 5)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (140, 'Medical',           'MedicalServices',  -3790808, 1, 5, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (141, 'Pharmacy',          'LocalPharmacy',    -3790808, 1, 5, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (142, 'Fitness',           'FitnessCenter',    -3790808, 1, 5, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (143, 'Mental & Wellbeing','SelfImprovement',  -3790808, 1, 5, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (144, 'Personal Care',     'Spa',              -3790808, 1, 5, 'want')

-- Health — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Doctor',          'LocalHospital',   -3790808, 1, 140, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Dentist',         'MedicalServices', -3790808, 1, 140, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hospital',        'LocalHospital',   -3790808, 1, 140, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vision',          'Visibility',      -3790808, 1, 140, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gym',             'FitnessCenter',   -3790808, 1, 142, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Sports',          'Sports',          -3790808, 1, 142, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fitness Classes', 'DirectionsRun',   -3790808, 1, 142, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Haircut',         'ContentCut',      -3790808, 1, 144, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Beauty',          'Face',            -3790808, 1, 144, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Spa & Massage',   'Spa',             -3790808, 1, 144, 'want')

-- ============================================================
-- Entertainment — Level 1  (parentId = 6)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (150, 'Movies & Theater', 'Movie',         -9623334, 1, 6, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (151, 'Music',            'MusicNote',     -9623334, 1, 6, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (152, 'Gaming',           'SportsEsports', -9623334, 1, 6, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (153, 'Events & Shows',   'EventNote',     -9623334, 1, 6, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (154, 'Hobbies',          'Palette',       -9623334, 1, 6, 'want')

-- Entertainment — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cinema',        'Theaters',       -9623334, 1, 150, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Streaming',     'PlayArrow',      -9623334, 1, 150, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Concerts',      'LibraryMusic',   -9623334, 1, 151, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Live Shows',    'TheaterComedy',  -9623334, 1, 153, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Sports Events', 'Stadium',        -9623334, 1, 153, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Arts & Crafts', 'Brush',          -9623334, 1, 154, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Photography',   'CameraAlt',      -9623334, 1, 154, 'want')

-- ============================================================
-- Travel — Level 1  (parentId = 7)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (160, 'Flights',          'Flight',        -16615491, 1, 7, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (161, 'Accommodation',    'Hotel',         -16615491, 1, 7, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (162, 'Ground Transport', 'DirectionsBus', -16615491, 1, 7, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (163, 'Activities',       'Attractions',   -16615491, 1, 7, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (164, 'Travel Essentials','Luggage',       -16615491, 1, 7, 'want')

-- Travel — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hotels',           'Hotel',          -16615491, 1, 161, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hostels',          'Bed',            -16615491, 1, 161, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vacation Rental',  'Home',           -16615491, 1, 161, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Rental',       'DirectionsCar',  -16615491, 1, 162, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Travel Insurance', 'Shield',         -16615491, 1, 164, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Visas & Permits',  'CardMembership', -16615491, 1, 164, 'want')

-- ============================================================
-- Education — Level 1  (parentId = 8)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (170, 'Tuition & Fees',  'School',        -15064194, 1, 8, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (171, 'Online Learning', 'OndemandVideo', -15064194, 1, 8, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (172, 'Books & Supplies','AutoStories',   -15064194, 1, 8, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (173, 'Childcare',       'ChildCare',     -15064194, 1, 8, 'need')

-- Education — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('University',     'AccountBalance', -15064194, 1, 170, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Training',       'Class',          -15064194, 1, 170, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Certifications', 'Verified',       -15064194, 1, 170, 'need')

-- ============================================================
-- Finance — Level 1  (parentId = 9)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (180, 'Savings',         'Savings',       -13407970, 1, 9, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (181, 'Investments',     'TrendingUp',    -13407970, 1, 9, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (182, 'Insurance',       'Shield',        -13407970, 1, 9, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (183, 'Taxes & Fees',    'Receipt',       -13407970, 1, 9, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (184, 'Loans & Debt',    'Money',         -13407970, 1, 9, 'must')

-- Finance — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Stocks',            'ShowChart',        -13407970, 1, 181, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Mutual Funds',      'BarChart',         -13407970, 1, 181, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Crypto',            'CurrencyBitcoin',  -13407970, 1, 181, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Real Estate',       'Domain',           -13407970, 1, 181, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bonds & Funds',     'AccountBalance',   -13407970, 1, 181, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Life Insurance',    'Favorite',         -13407970, 1, 182, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vehicle Insurance', 'DirectionsCar',    -13407970, 1, 182, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Property Insurance','Home',             -13407970, 1, 182, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Student Loan',      'School',           -13407970, 1, 184, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Credit Card Debt',  'CreditCard',       -13407970, 1, 184, 'must')

-- ============================================================
-- Subscriptions — Level 1  (parentId = 10)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (185, 'Video Streaming',       'PlayArrow',      -13558894, 1, 10, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (186, 'Music & Audio',         'MusicNote',      -13558894, 1, 10, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (187, 'Software Subscriptions','Laptop',         -13558894, 1, 10, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (188, 'News & Reading',        'Article',        -13558894, 1, 10, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (189, 'Memberships',           'CardMembership', -13558894, 1, 10, 'want')

-- Subscriptions — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cloud Storage', 'Cloud',     -13558894, 1, 187, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gaming Pass',   'Gamepad',   -13558894, 1, 187, 'want')

-- ============================================================
-- Gifts & Charity — Level 1  (parentId = 11)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (190, 'Gifts',          'CardGiftcard',      -7860657, 1, 11, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (191, 'Donations',      'VolunteerActivism', -7860657, 1, 11, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (192, 'Family Support', 'FamilyRestroom',    -7860657, 1, 11, 'want')

-- ============================================================
-- Business — Level 1  (parentId = 12)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (193, 'Office & Supplies',     'BusinessCenter', -13154481, 1, 12, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (194, 'Marketing',             'Campaign',       -13154481, 1, 12, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (195, 'Professional Services', 'Gavel',          -13154481, 1, 12, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (196, 'Client Entertainment',  'Groups',         -13154481, 1, 12, 'want')

-- ============================================================
-- Income — Level 1  (parentId = 13)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (197, 'Employment',         'Work',       -14983648, 1, 13, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (198, 'Freelance',          'Handshake',  -14983648, 1, 13, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (199, 'Investment Returns', 'TrendingUp', -14983648, 1, 13, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (200, 'Other Income',       'Payments',   -14983648, 1, 13, 'need')

-- Income — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Salary',              'Payments',       -14983648, 1, 197, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bonus',               'Star',           -14983648, 1, 197, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Commission',          'Percent',        -14983648, 1, 197, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Dividends',           'AccountBalance', -14983648, 1, 199, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Capital Gains',       'TrendingUp',     -14983648, 1, 199, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Rental Income',       'Home',           -14983648, 1, 199, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Refunds & Cashback',  'ReceiptLong',    -14983648, 1, 200, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Tax Refund',          'AccountBalance', -14983648, 1, 200, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Government Benefits', 'AccountBalance', -14983648, 1, 200, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gifts Received',      'CardGiftcard',   -14983648, 1, 200, 'need')

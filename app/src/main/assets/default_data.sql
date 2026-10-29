-- ============================================================
-- OssWallet default category taxonomy — v3
-- Designed fresh (not a mechanical fold of BudgetBakers' hierarchy):
-- keeps Health & Wellness standalone, avoids duplicate/near-duplicate
-- categories, uses plain names. Borrows the genuinely good structural
-- ideas (Transport/Vehicle split, Communication & Technology root,
-- Finance/Investments split, domain-specific insurance) without
-- copying BudgetBakers' literal label strings.
-- Reserved ids 1000-1005 (Others/Missing/Unknown/InitialBalance/Transfer)
-- are injected by ensureSystemCategories() in AppDatabase.kt BEFORE this
-- file runs — never redefine them here.
-- ============================================================

-- ============================================================
-- Root Categories (15)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1006, 'Food & Drink', 'Restaurant', -689152, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1007, 'Shopping', 'ShoppingBag', -8708190, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1008, 'Housing', 'Home', -16750244, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1009, 'Transportation', 'DirectionsBus', -15374912, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1010, 'Vehicle', 'DirectionsCar', -6543440, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1011, 'Health & Wellness', 'HealthAndSafety', -3790808, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1012, 'Entertainment & Leisure', 'Movie', -9623334, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1013, 'Travel', 'Flight', -16615491, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1014, 'Education', 'School', -15064194, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1015, 'Communication & Technology', 'Computer', -14776091, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1016, 'Finance & Insurance', 'AccountBalance', -13407970, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1017, 'Investments', 'TrendingUp', -1499549, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1018, 'Gifts & Charity', 'CardGiftcard', -7860657, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1019, 'Business', 'Work', -13154481, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1020, 'Income', 'Payments', -14983648, 1, NULL, 'need')

-- ============================================================
-- Food & Drink (parentId = 1006)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1021, 'Groceries', 'LocalGroceryStore', -689152, 1, 1006, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1022, 'Dining Out', 'LocalDining', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1023, 'Coffee & Snacks', 'Coffee', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1024, 'Bars & Nightlife', 'LocalBar', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1025, 'Food Delivery', 'DeliveryDining', -689152, 1, 1006, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1026, 'Supermarket', 'Store', -689152, 1, 1021, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1027, 'Specialty Food', 'Storefront', -689152, 1, 1021, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1028, 'Restaurants', 'Restaurant', -689152, 1, 1022, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1029, 'Fast Food', 'Fastfood', -689152, 1, 1022, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1030, 'Alcohol', 'Liquor', -689152, 1, 1024, 'want')

-- ============================================================
-- Shopping (parentId = 1007)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1031, 'Clothing & Apparel', 'Checkroom', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1032, 'Electronics', 'Devices', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1033, 'Home & Living', 'Chair', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1034, 'Books & Stationery', 'MenuBook', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1035, 'Online Shopping', 'ShoppingCart', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1036, 'Pets & Animals', 'Pets', -8708190, 1, 1007, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1037, 'Clothing', 'Checkroom', -8708190, 1, 1031, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1038, 'Shoes', 'Hiking', -8708190, 1, 1031, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1039, 'Accessories', 'Diamond', -8708190, 1, 1031, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1040, 'Phones', 'Smartphone', -8708190, 1, 1032, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1041, 'Computers', 'LaptopMac', -8708190, 1, 1032, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1042, 'Furniture', 'Chair', -8708190, 1, 1033, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1043, 'Appliances', 'Kitchen', -8708190, 1, 1033, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1044, 'Home & Garden', 'LocalFlorist', -8708190, 1, 1033, 'want')

-- ============================================================
-- Housing (parentId = 1008)
-- "Furnishing" dropped: it duplicated Shopping/Home & Living/Furniture
-- for the same real-world purchase. Internet/Phone moved out to
-- Communication & Technology (first-class subs there, not duplicated
-- here). Childcare folded in (recurring family bill, same bucket as
-- Rent/Utilities). Property Insurance kept here, not in a generic
-- Finance bucket, matching Vehicle/Travel's domain-specific insurance.
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1045, 'Rent', 'Key', -16750244, 1, 1008, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1046, 'Mortgage', 'HomeWork', -16750244, 1, 1008, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1047, 'Utilities', 'Bolt', -16750244, 1, 1008, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1048, 'Home Services', 'HomeRepairService', -16750244, 1, 1008, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1049, 'Childcare', 'FamilyRestroom', -16750244, 1, 1008, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1050, 'Property Insurance', 'Shield', -16750244, 1, 1008, 'must')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1051, 'Electricity', 'Lightbulb', -16750244, 1, 1047, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1052, 'Water', 'WaterDrop', -16750244, 1, 1047, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1053, 'Gas', 'LocalFireDepartment', -16750244, 1, 1047, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1054, 'Cleaning', 'CleaningServices', -16750244, 1, 1048, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1055, 'Repairs', 'Build', -16750244, 1, 1048, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1056, 'Security', 'Security', -16750244, 1, 1048, 'need')

-- ============================================================
-- Transportation (parentId = 1009) — daily commute, public/active
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1057, 'Public Transit', 'DirectionsBus', -15374912, 1, 1009, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1058, 'Ride & Taxi', 'LocalTaxi', -15374912, 1, 1009, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1059, 'Cycling', 'PedalBike', -15374912, 1, 1009, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1060, 'Bus & Metro', 'DirectionsBusFilled', -15374912, 1, 1057, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1061, 'Train', 'Train', -15374912, 1, 1057, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1062, 'Rideshare', 'Hail', -15374912, 1, 1058, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1063, 'Taxi', 'LocalTaxi', -15374912, 1, 1058, 'want')

-- ============================================================
-- Vehicle (parentId = 1010) — costs of owning a vehicle
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1064, 'Fuel', 'LocalGasStation', -6543440, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1065, 'Parking', 'LocalParking', -6543440, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1066, 'Vehicle Maintenance', 'Build', -6543440, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1067, 'Vehicle Insurance', 'Shield', -6543440, 1, 1010, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1068, 'Rentals & Leasing', 'RequestQuote', -6543440, 1, 1010, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1069, 'Car Wash', 'LocalCarWash', -6543440, 1, 1066, 'want')

-- ============================================================
-- Health & Wellness (parentId = 1011) — kept standalone on purpose
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1070, 'Medical Care', 'MedicalServices', -3790808, 1, 1011, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1071, 'Pharmacy', 'LocalPharmacy', -3790808, 1, 1011, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1072, 'Fitness', 'FitnessCenter', -3790808, 1, 1011, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1073, 'Mental & Wellbeing', 'SelfImprovement', -3790808, 1, 1011, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1074, 'Personal Care', 'Spa', -3790808, 1, 1011, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1075, 'Doctor', 'LocalHospital', -3790808, 1, 1070, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1076, 'Dentist', 'Healing', -3790808, 1, 1070, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1077, 'Hospital', 'Emergency', -3790808, 1, 1070, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1078, 'Vision', 'Visibility', -3790808, 1, 1070, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1079, 'Health Insurance', 'Shield', -3790808, 1, 1070, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1080, 'Gym', 'FitnessCenter', -3790808, 1, 1072, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1081, 'Sports', 'Sports', -3790808, 1, 1072, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1082, 'Fitness Classes', 'DirectionsRun', -3790808, 1, 1072, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1083, 'Haircut', 'ContentCut', -3790808, 1, 1074, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1084, 'Beauty', 'Face', -3790808, 1, 1074, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1085, 'Spa & Massage', 'Spa', -3790808, 1, 1074, 'want')

-- ============================================================
-- Entertainment & Leisure (parentId = 1012)
-- "Video Streaming"/"Music & Audio"/"Memberships" dropped: each was a
-- near-duplicate of a category already here (Streaming, Music, and
-- Memberships' own parent respectively).
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1086, 'Movies & Theater', 'Theaters', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1087, 'Music', 'MusicNote', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1088, 'Gaming', 'SportsEsports', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1089, 'Events & Shows', 'EventNote', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1090, 'Hobbies', 'Palette', -9623334, 1, 1012, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1091, 'Cinema', 'LocalMovies', -9623334, 1, 1086, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1092, 'Streaming', 'LiveTv', -9623334, 1, 1086, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1093, 'Concerts', 'Festival', -9623334, 1, 1087, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1094, 'Live Shows', 'TheaterComedy', -9623334, 1, 1089, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1095, 'Sports Events', 'Stadium', -9623334, 1, 1089, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1096, 'Arts & Crafts', 'Brush', -9623334, 1, 1090, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1097, 'Photography', 'CameraAlt', -9623334, 1, 1090, 'want')

-- ============================================================
-- Travel (parentId = 1013)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1098, 'Flights', 'Flight', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1099, 'Accommodation', 'Hotel', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1100, 'Ground Transport', 'DirectionsBus', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1101, 'Activities', 'Attractions', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1102, 'Travel Essentials', 'Luggage', -16615491, 1, 1013, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1103, 'Hotels', 'Hotel', -16615491, 1, 1099, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1104, 'Hostels', 'Bed', -16615491, 1, 1099, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1105, 'Vacation Rental', 'House', -16615491, 1, 1099, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1106, 'Car Rental', 'DirectionsCar', -16615491, 1, 1100, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1107, 'Travel Insurance', 'Shield', -16615491, 1, 1102, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1108, 'Visas & Permits', 'CardTravel', -16615491, 1, 1102, 'want')

-- ============================================================
-- Education (parentId = 1014)
-- Childcare moved out to Housing (recurring family bill, not learning).
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1109, 'Tuition & Fees', 'School', -15064194, 1, 1014, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1110, 'Online Learning', 'OndemandVideo', -15064194, 1, 1014, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1111, 'Books & Supplies', 'AutoStories', -15064194, 1, 1014, 'need')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1112, 'University', 'AccountBalance', -15064194, 1, 1109, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1113, 'Training', 'Class', -15064194, 1, 1109, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1114, 'Certifications', 'Verified', -15064194, 1, 1109, 'need')

-- ============================================================
-- Communication & Technology (parentId = 1015)
-- New root; Internet/Phone moved here from Housing (first-class, not
-- duplicated in both places).
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1115, 'Internet', 'Wifi', -14776091, 1, 1015, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1116, 'Phone & Mobile', 'Phone', -14776091, 1, 1015, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1117, 'Software & Subscriptions', 'Laptop', -14776091, 1, 1015, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1118, 'Cloud Storage', 'Cloud', -14776091, 1, 1117, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1119, 'Gaming Pass', 'Gamepad', -14776091, 1, 1117, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1120, 'News & Reading', 'Article', -14776091, 1, 1117, 'want')

-- ============================================================
-- Finance & Insurance (parentId = 1016) — bills, not assets
-- Domain-specific insurance (Vehicle/Travel/Property/Health) lives in
-- its own root; only Life Insurance (no better home) is here.
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1121, 'Insurance', 'Shield', -13407970, 1, 1016, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1122, 'Taxes & Fees', 'Receipt', -13407970, 1, 1016, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1123, 'Loans & Debt', 'Money', -13407970, 1, 1016, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1124, 'Bank Fees & Fines', 'CurrencyExchange', -13407970, 1, 1016, 'must')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1125, 'Life Insurance', 'Shield', -13407970, 1, 1121, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1126, 'Student Loan', 'School', -13407970, 1, 1123, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1127, 'Credit Card Debt', 'CreditCard', -13407970, 1, 1123, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1128, 'Fines', 'Gavel', -13407970, 1, 1124, 'must')

-- ============================================================
-- Investments (parentId = 1017) — assets, not bills
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1129, 'Savings', 'Savings', -1499549, 1, 1017, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1130, 'Stocks & Funds', 'ShowChart', -1499549, 1, 1017, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1131, 'Real Estate', 'Domain', -1499549, 1, 1017, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1132, 'Crypto', 'CurrencyBitcoin', -1499549, 1, 1017, 'want')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1133, 'Retirement', 'Savings', -1499549, 1, 1129, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1134, 'Mutual Funds', 'BarChart', -1499549, 1, 1130, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1135, 'Bonds & Funds', 'AccountBalance', -1499549, 1, 1130, 'want')

-- ============================================================
-- Gifts & Charity (parentId = 1018)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1136, 'Gifts', 'CardGiftcard', -7860657, 1, 1018, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1137, 'Donations', 'VolunteerActivism', -7860657, 1, 1018, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1138, 'Family Support', 'FamilyRestroom', -7860657, 1, 1018, 'want')

-- ============================================================
-- Business (parentId = 1019) — preserved verbatim, no Wallet equivalent
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1139, 'Office & Supplies', 'BusinessCenter', -13154481, 1, 1019, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1140, 'Marketing', 'Campaign', -13154481, 1, 1019, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1141, 'Professional Services', 'Gavel', -13154481, 1, 1019, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1142, 'Client Entertainment', 'Groups', -13154481, 1, 1019, 'want')

-- ============================================================
-- Income (parentId = 1020)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1143, 'Employment', 'Work', -14983648, 1, 1020, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1144, 'Freelance', 'Handshake', -14983648, 1, 1020, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1145, 'Investment Returns', 'TrendingUp', -14983648, 1, 1020, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1146, 'Other Income', 'Payments', -14983648, 1, 1020, 'need')

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1147, 'Salary', 'Payments', -14983648, 1, 1143, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1148, 'Bonus', 'Star', -14983648, 1, 1143, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1149, 'Commission', 'Percent', -14983648, 1, 1143, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1150, 'Dividends', 'AccountBalance', -14983648, 1, 1145, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1151, 'Capital Gains', 'TrendingUp', -14983648, 1, 1145, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1152, 'Rental Income', 'Home', -14983648, 1, 1145, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1153, 'Business Income', 'BusinessCenter', -14983648, 1, 1146, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1154, 'Refunds & Cashback', 'ReceiptLong', -14983648, 1, 1146, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1155, 'Tax Refund', 'AccountBalance', -14983648, 1, 1146, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1156, 'Government Benefits', 'AccountBalance', -14983648, 1, 1146, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1157, 'Gifts Received', 'CardGiftcard', -14983648, 1, 1146, 'need')

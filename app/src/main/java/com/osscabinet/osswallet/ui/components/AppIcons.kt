package com.osscabinet.ossWallet.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.AirportShuttle
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BreakfastDining
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.BrunchDining
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Cabin
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CardTravel
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.CorporateFare
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Countertops
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsBusFilled
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsCarFilled
import androidx.compose.material.icons.rounded.DirectionsSubway
import androidx.compose.material.icons.rounded.Discount
import androidx.compose.material.icons.rounded.Diversity1
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.ElectricCar
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EmojiFoodBeverage
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Euro
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Festival
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Fireplace
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FlightLand
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.FoodBank
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Foundation
import androidx.compose.material.icons.rounded.GasMeter
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.HeatPump
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeRepairService
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.Hvac
import androidx.compose.material.icons.rounded.IceSkating
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Kayaking
import androidx.compose.material.icons.rounded.KebabDining
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.KingBed
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Liquor
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalConvenienceStore
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.LocalMovies
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Loyalty
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.MoneyOff
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Moped
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Motorcycle
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nature
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.PriceCheck
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Sports
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Tram
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

val AppIconMap: Map<String, ImageVector> by lazy {
    mapOf(
        // Finance & Accounts
        "AccountBalance" to Icons.Rounded.AccountBalance,
        "AccountBalanceWallet" to Icons.Rounded.AccountBalanceWallet,
        "AccountTree" to Icons.Rounded.AccountTree,
        "AttachMoney" to Icons.Rounded.AttachMoney,
        "AutoGraph" to Icons.Rounded.AutoGraph,
        "Analytics" to Icons.Rounded.Analytics,
        "BarChart" to Icons.Rounded.BarChart,
        "CardGiftcard" to Icons.Rounded.CardGiftcard,
        "CardTravel" to Icons.Rounded.CardTravel,
        "CreditCard" to Icons.Rounded.CreditCard,
        "CreditScore" to Icons.Rounded.CreditScore,
        "CurrencyBitcoin" to Icons.Rounded.CurrencyBitcoin,
        "CurrencyExchange" to Icons.Rounded.CurrencyExchange,
        "Diamond" to Icons.Rounded.Diamond,
        "Euro" to Icons.Rounded.Euro,
        "Foundation" to Icons.Rounded.Foundation,
        "LocalAtm" to Icons.Rounded.LocalAtm,
        "Loyalty" to Icons.Rounded.Loyalty,
        "Money" to Icons.Rounded.Money,
        "MonetizationOn" to Icons.Rounded.MonetizationOn,
        "MoneyOff" to Icons.Rounded.MoneyOff,
        "Paid" to Icons.Rounded.Paid,
        "Payments" to Icons.Rounded.Payments,
        "Percent" to Icons.Rounded.Percent,
        "PointOfSale" to Icons.Rounded.PointOfSale,
        "PriceCheck" to Icons.Rounded.PriceCheck,
        "RequestQuote" to Icons.Rounded.RequestQuote,
        "RocketLaunch" to Icons.Rounded.RocketLaunch,
        "Savings" to Icons.Rounded.Savings,
        "Sell" to Icons.Rounded.Sell,
        "ShowChart" to Icons.AutoMirrored.Rounded.ShowChart,
        "Stars" to Icons.Rounded.Stars,
        "TrendingDown" to Icons.AutoMirrored.Rounded.TrendingDown,
        "TrendingFlat" to Icons.AutoMirrored.Rounded.TrendingFlat,
        "TrendingUp" to Icons.AutoMirrored.Rounded.TrendingUp,
        "Wallet" to Icons.Rounded.Wallet,

        // Food & Dining
        "BakeryDining" to Icons.Rounded.BakeryDining,
        "BreakfastDining" to Icons.Rounded.BreakfastDining,
        "BrunchDining" to Icons.Rounded.BrunchDining,
        "Cake" to Icons.Rounded.Cake,
        "Coffee" to Icons.Rounded.Coffee,
        "DinnerDining" to Icons.Rounded.DinnerDining,
        "EmojiFoodBeverage" to Icons.Rounded.EmojiFoodBeverage,
        "Fastfood" to Icons.Rounded.Fastfood,
        "FoodBank" to Icons.Rounded.FoodBank,
        "Icecream" to Icons.Rounded.Icecream,
        "KebabDining" to Icons.Rounded.KebabDining,
        "Liquor" to Icons.Rounded.Liquor,
        "LocalBar" to Icons.Rounded.LocalBar,
        "LocalCafe" to Icons.Rounded.LocalCafe,
        "LocalDrink" to Icons.Rounded.LocalDrink,
        "LocalPizza" to Icons.Rounded.LocalPizza,
        "LunchDining" to Icons.Rounded.LunchDining,
        "Restaurant" to Icons.Rounded.Restaurant,

        // Shopping & Retail
        "AddShoppingCart" to Icons.Rounded.AddShoppingCart,
        "Discount" to Icons.Rounded.Discount,
        "LocalConvenienceStore" to Icons.Rounded.LocalConvenienceStore,
        "LocalGroceryStore" to Icons.Rounded.LocalGroceryStore,
        "LocalMall" to Icons.Rounded.LocalMall,
        "LocalOffer" to Icons.Rounded.LocalOffer,
        "ShoppingBag" to Icons.Rounded.ShoppingBag,
        "ShoppingBasket" to Icons.Rounded.ShoppingBasket,
        "ShoppingCart" to Icons.Rounded.ShoppingCart,
        "Store" to Icons.Rounded.Store,
        "Storefront" to Icons.Rounded.Storefront,

        // Transportation
        "AirportShuttle" to Icons.Rounded.AirportShuttle,
        "DirectionsBike" to Icons.AutoMirrored.Rounded.DirectionsBike,
        "DirectionsBus" to Icons.Rounded.DirectionsBus,
        "DirectionsBusFilled" to Icons.Rounded.DirectionsBusFilled,
        "DirectionsCar" to Icons.Rounded.DirectionsCar,
        "DirectionsCarFilled" to Icons.Rounded.DirectionsCarFilled,
        "DirectionsRun" to Icons.AutoMirrored.Rounded.DirectionsRun,
        "DirectionsSubway" to Icons.Rounded.DirectionsSubway,
        "DirectionsWalk" to Icons.AutoMirrored.Rounded.DirectionsWalk,
        "ElectricCar" to Icons.Rounded.ElectricCar,
        "Flight" to Icons.Rounded.Flight,
        "FlightLand" to Icons.Rounded.FlightLand,
        "FlightTakeoff" to Icons.Rounded.FlightTakeoff,
        "LocalGasStation" to Icons.Rounded.LocalGasStation,
        "LocalTaxi" to Icons.Rounded.LocalTaxi,
        "Moped" to Icons.Rounded.Moped,
        "Motorcycle" to Icons.Rounded.Motorcycle,
        "Train" to Icons.Rounded.Train,
        "Tram" to Icons.Rounded.Tram,
        "TwoWheeler" to Icons.Rounded.TwoWheeler,

        // Housing & Home
        "Apartment" to Icons.Rounded.Apartment,
        "Bathtub" to Icons.Rounded.Bathtub,
        "Build" to Icons.Rounded.Build,
        "Cabin" to Icons.Rounded.Cabin,
        "Construction" to Icons.Rounded.Construction,
        "Cottage" to Icons.Rounded.Cottage,
        "Countertops" to Icons.Rounded.Countertops,
        "ElectricalServices" to Icons.Rounded.ElectricalServices,
        "Fireplace" to Icons.Rounded.Fireplace,
        "Handyman" to Icons.Rounded.Handyman,
        "Home" to Icons.Rounded.Home,
        "HomeRepairService" to Icons.Rounded.HomeRepairService,
        "HomeWork" to Icons.Rounded.HomeWork,
        "House" to Icons.Rounded.House,
        "Kitchen" to Icons.Rounded.Kitchen,
        "KingBed" to Icons.Rounded.KingBed,
        "Plumbing" to Icons.Rounded.Plumbing,

        // Health & Medical
        "Bloodtype" to Icons.Rounded.Bloodtype,
        "Emergency" to Icons.Rounded.Emergency,
        "FitnessCenter" to Icons.Rounded.FitnessCenter,
        "Healing" to Icons.Rounded.Healing,
        "HealthAndSafety" to Icons.Rounded.HealthAndSafety,
        "LocalHospital" to Icons.Rounded.LocalHospital,
        "LocalPharmacy" to Icons.Rounded.LocalPharmacy,
        "Medication" to Icons.Rounded.Medication,
        "MedicalServices" to Icons.Rounded.MedicalServices,
        "MonitorHeart" to Icons.Rounded.MonitorHeart,
        "Spa" to Icons.Rounded.Spa,
        "Vaccines" to Icons.Rounded.Vaccines,

        // Entertainment
        "Audiotrack" to Icons.Rounded.Audiotrack,
        "Casino" to Icons.Rounded.Casino,
        "ConfirmationNumber" to Icons.Rounded.ConfirmationNumber,
        "Festival" to Icons.Rounded.Festival,
        "Headphones" to Icons.Rounded.Headphones,
        "LiveTv" to Icons.Rounded.LiveTv,
        "LocalMovies" to Icons.Rounded.LocalMovies,
        "MicNone" to Icons.Rounded.MicNone,
        "Movie" to Icons.Rounded.Movie,
        "MusicNote" to Icons.Rounded.MusicNote,
        "SportsEsports" to Icons.Rounded.SportsEsports,
        "TheaterComedy" to Icons.Rounded.TheaterComedy,
        "Theaters" to Icons.Rounded.Theaters,

        // Education & Learning
        "AutoStories" to Icons.Rounded.AutoStories,
        "Book" to Icons.Rounded.Book,
        "Calculate" to Icons.Rounded.Calculate,
        "HistoryEdu" to Icons.Rounded.HistoryEdu,
        "LocalLibrary" to Icons.Rounded.LocalLibrary,
        "MenuBook" to Icons.AutoMirrored.Rounded.MenuBook,
        "School" to Icons.Rounded.School,
        "Science" to Icons.Rounded.Science,

        // Work & Business
        "Agriculture" to Icons.Rounded.Agriculture,
        "BusinessCenter" to Icons.Rounded.BusinessCenter,
        "CorporateFare" to Icons.Rounded.CorporateFare,
        "Engineering" to Icons.Rounded.Engineering,
        "Factory" to Icons.Rounded.Factory,
        "Gavel" to Icons.Rounded.Gavel,
        "ManageAccounts" to Icons.Rounded.ManageAccounts,
        "Work" to Icons.Rounded.Work,

        // Travel & Tourism
        "BeachAccess" to Icons.Rounded.BeachAccess,
        "Explore" to Icons.Rounded.Explore,
        "Hotel" to Icons.Rounded.Hotel,
        "Luggage" to Icons.Rounded.Luggage,
        "Map" to Icons.Rounded.Map,
        "Place" to Icons.Rounded.Place,
        "TravelExplore" to Icons.Rounded.TravelExplore,

        // Sports & Recreation
        "EmojiEvents" to Icons.Rounded.EmojiEvents,
        "Hiking" to Icons.Rounded.Hiking,
        "IceSkating" to Icons.Rounded.IceSkating,
        "Kayaking" to Icons.Rounded.Kayaking,
        "Pool" to Icons.Rounded.Pool,
        "Sports" to Icons.Rounded.Sports,
        "SportsBasketball" to Icons.Rounded.SportsBasketball,
        "SportsSoccer" to Icons.Rounded.SportsSoccer,
        "SportsTennis" to Icons.Rounded.SportsTennis,

        // Utilities & Services
        "Bolt" to Icons.Rounded.Bolt,
        "Cable" to Icons.Rounded.Cable,
        "ElectricBolt" to Icons.Rounded.ElectricBolt,
        "FlashOn" to Icons.Rounded.FlashOn,
        "GasMeter" to Icons.Rounded.GasMeter,
        "HeatPump" to Icons.Rounded.HeatPump,
        "Hvac" to Icons.Rounded.Hvac,
        "Water" to Icons.Rounded.Water,
        "WaterDrop" to Icons.Rounded.WaterDrop,
        "Wifi" to Icons.Rounded.Wifi,

        // Technology & Devices
        "Bluetooth" to Icons.Rounded.Bluetooth,
        "CameraAlt" to Icons.Rounded.CameraAlt,
        "Cloud" to Icons.Rounded.Cloud,
        "Code" to Icons.Rounded.Code,
        "Computer" to Icons.Rounded.Computer,
        "Laptop" to Icons.Rounded.Laptop,
        "Print" to Icons.Rounded.Print,
        "Smartphone" to Icons.Rounded.Smartphone,
        "Terminal" to Icons.Rounded.Terminal,

        // People & Social
        "ChildCare" to Icons.Rounded.ChildCare,
        "Diversity1" to Icons.Rounded.Diversity1,
        "Diversity3" to Icons.Rounded.Diversity3,
        "Face" to Icons.Rounded.Face,
        "Favorite" to Icons.Rounded.Favorite,
        "Group" to Icons.Rounded.Group,
        "GroupAdd" to Icons.Rounded.GroupAdd,
        "Groups" to Icons.Rounded.Groups,
        "Person" to Icons.Rounded.Person,
        "Pets" to Icons.Rounded.Pets,

        // Communication
        "Call" to Icons.Rounded.Call,
        "Chat" to Icons.AutoMirrored.Rounded.Chat,
        "Email" to Icons.Rounded.Email,
        "Forum" to Icons.Rounded.Forum,
        "Language" to Icons.Rounded.Language,
        "Notifications" to Icons.Rounded.Notifications,
        "Phone" to Icons.Rounded.Phone,
        "Public" to Icons.Rounded.Public,

        // Nature & Outdoors
        "DarkMode" to Icons.Rounded.DarkMode,
        "Eco" to Icons.Rounded.Eco,
        "Forest" to Icons.Rounded.Forest,
        "Grass" to Icons.Rounded.Grass,
        "Lightbulb" to Icons.Rounded.Lightbulb,
        "LocalFlorist" to Icons.Rounded.LocalFlorist,
        "Nature" to Icons.Rounded.Nature,
        "Park" to Icons.Rounded.Park,
        "WbSunny" to Icons.Rounded.WbSunny,

        // UI & System (required by domain models and hardcoded references)
        "Add" to Icons.Rounded.Add,
        "Archive" to Icons.Rounded.Archive,
        "ArrowDownward" to Icons.Rounded.ArrowDownward,
        "ArrowUpward" to Icons.Rounded.ArrowUpward,
        "AudioFile" to Icons.Rounded.AudioFile,
        "Autorenew" to Icons.Rounded.Autorenew,
        "Bookmark" to Icons.Rounded.Bookmark,
        "BrokenImage" to Icons.Rounded.BrokenImage,
        "BugReport" to Icons.Rounded.BugReport,
        "Cancel" to Icons.Rounded.Cancel,
        "Category" to Icons.Rounded.Category,
        "CalendarMonth" to Icons.Rounded.CalendarMonth,
        "CalendarToday" to Icons.Rounded.CalendarToday,
        "Check" to Icons.Rounded.Check,
        "CheckCircle" to Icons.Rounded.CheckCircle,
        "Close" to Icons.Rounded.Close,
        "Contactless" to Icons.Rounded.Contactless,
        "Description" to Icons.Rounded.Description,
        "Done" to Icons.Rounded.Done,
        "Download" to Icons.Rounded.Download,
        "Edit" to Icons.Rounded.Edit,
        "Error" to Icons.Rounded.Error,
        "Event" to Icons.Rounded.Event,
        "FileOpen" to Icons.Rounded.FileOpen,
        "Fingerprint" to Icons.Rounded.Fingerprint,
        "Flag" to Icons.Rounded.Flag,
        "Help" to Icons.AutoMirrored.Rounded.Help,
        "Info" to Icons.Rounded.Info,
        "Inventory" to Icons.Rounded.Inventory,
        "Key" to Icons.Rounded.Key,
        "Label" to Icons.AutoMirrored.Rounded.Label,
        "ListAlt" to Icons.AutoMirrored.Rounded.ListAlt,
        "Lock" to Icons.Rounded.Lock,
        "Menu" to Icons.Rounded.Menu,
        "MoreHoriz" to Icons.Rounded.MoreHoriz,
        "PushPin" to Icons.Rounded.PushPin,
        "QuestionMark" to Icons.Rounded.QuestionMark,
        "RadioButtonUnchecked" to Icons.Rounded.RadioButtonUnchecked,
        "Receipt" to Icons.Rounded.Receipt,
        "ReceiptLong" to Icons.AutoMirrored.Rounded.ReceiptLong,
        "Refresh" to Icons.Rounded.Refresh,
        "Remove" to Icons.Rounded.Remove,
        "Repeat" to Icons.Rounded.Repeat,
        "Schedule" to Icons.Rounded.Schedule,
        "Search" to Icons.Rounded.Search,
        "Security" to Icons.Rounded.Security,
        "Settings" to Icons.Rounded.Settings,
        "Shield" to Icons.Rounded.Shield,
        "Star" to Icons.Rounded.Star,
        "SubdirectoryArrowRight" to Icons.Rounded.SubdirectoryArrowRight,
        "SwapHoriz" to Icons.Rounded.SwapHoriz,
        "Sync" to Icons.Rounded.Sync,
        "Tag" to Icons.Rounded.Tag,
        "TaskAlt" to Icons.Rounded.TaskAlt,
        "Timer" to Icons.Rounded.Timer,
        "Today" to Icons.Rounded.Today,
        "VpnKey" to Icons.Rounded.VpnKey,
        "Warning" to Icons.Rounded.Warning,
        "Policy" to Icons.Rounded.Policy,
    )
}

fun getIconByName(name: String?): ImageVector =
    AppIconMap[name] ?: Icons.Rounded.Category

fun getAccountIconByName(name: String?): ImageVector =
    AppIconMap[name] ?: Icons.Rounded.AccountBalance

val com.osscabinet.ossWallet.domain.model.AccountStructureType.icon: ImageVector
    get() = AppIconMap[this.iconKey] ?: Icons.Rounded.Layers

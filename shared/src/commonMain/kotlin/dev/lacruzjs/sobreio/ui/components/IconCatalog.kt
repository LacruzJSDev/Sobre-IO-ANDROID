package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.CurrencyPound
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.ElderlyWoman
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RealEstateAgent
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.icon_apartment
import dev.lacruzjs.sobreio.generated.resources.icon_appliances
import dev.lacruzjs.sobreio.generated.resources.icon_atm
import dev.lacruzjs.sobreio.generated.resources.icon_baby
import dev.lacruzjs.sobreio.generated.resources.icon_bakery
import dev.lacruzjs.sobreio.generated.resources.icon_bank
import dev.lacruzjs.sobreio.generated.resources.icon_bank_fees
import dev.lacruzjs.sobreio.generated.resources.icon_bar
import dev.lacruzjs.sobreio.generated.resources.icon_beach
import dev.lacruzjs.sobreio.generated.resources.icon_beauty
import dev.lacruzjs.sobreio.generated.resources.icon_bed
import dev.lacruzjs.sobreio.generated.resources.icon_bike
import dev.lacruzjs.sobreio.generated.resources.icon_bills
import dev.lacruzjs.sobreio.generated.resources.icon_birthday
import dev.lacruzjs.sobreio.generated.resources.icon_bitcoin
import dev.lacruzjs.sobreio.generated.resources.icon_books
import dev.lacruzjs.sobreio.generated.resources.icon_bus
import dev.lacruzjs.sobreio.generated.resources.icon_business
import dev.lacruzjs.sobreio.generated.resources.icon_business_account
import dev.lacruzjs.sobreio.generated.resources.icon_camping
import dev.lacruzjs.sobreio.generated.resources.icon_candlestick
import dev.lacruzjs.sobreio.generated.resources.icon_car
import dev.lacruzjs.sobreio.generated.resources.icon_car_repair
import dev.lacruzjs.sobreio.generated.resources.icon_cash
import dev.lacruzjs.sobreio.generated.resources.icon_celebration
import dev.lacruzjs.sobreio.generated.resources.icon_chart
import dev.lacruzjs.sobreio.generated.resources.icon_christmas
import dev.lacruzjs.sobreio.generated.resources.icon_cinema
import dev.lacruzjs.sobreio.generated.resources.icon_cleaning
import dev.lacruzjs.sobreio.generated.resources.icon_clothes
import dev.lacruzjs.sobreio.generated.resources.icon_coffee
import dev.lacruzjs.sobreio.generated.resources.icon_computer
import dev.lacruzjs.sobreio.generated.resources.icon_contactless
import dev.lacruzjs.sobreio.generated.resources.icon_cottage
import dev.lacruzjs.sobreio.generated.resources.icon_couple
import dev.lacruzjs.sobreio.generated.resources.icon_courses
import dev.lacruzjs.sobreio.generated.resources.icon_credit_card
import dev.lacruzjs.sobreio.generated.resources.icon_currency_exchange
import dev.lacruzjs.sobreio.generated.resources.icon_debit_card
import dev.lacruzjs.sobreio.generated.resources.icon_delivery
import dev.lacruzjs.sobreio.generated.resources.icon_diamond
import dev.lacruzjs.sobreio.generated.resources.icon_doctor
import dev.lacruzjs.sobreio.generated.resources.icon_dollar
import dev.lacruzjs.sobreio.generated.resources.icon_donations
import dev.lacruzjs.sobreio.generated.resources.icon_elderly_care
import dev.lacruzjs.sobreio.generated.resources.icon_electricity
import dev.lacruzjs.sobreio.generated.resources.icon_electronics
import dev.lacruzjs.sobreio.generated.resources.icon_emergency_fund
import dev.lacruzjs.sobreio.generated.resources.icon_euro
import dev.lacruzjs.sobreio.generated.resources.icon_ev_charging
import dev.lacruzjs.sobreio.generated.resources.icon_family
import dev.lacruzjs.sobreio.generated.resources.icon_fast_food
import dev.lacruzjs.sobreio.generated.resources.icon_flag
import dev.lacruzjs.sobreio.generated.resources.icon_flight
import dev.lacruzjs.sobreio.generated.resources.icon_freelance
import dev.lacruzjs.sobreio.generated.resources.icon_friends
import dev.lacruzjs.sobreio.generated.resources.icon_fuel
import dev.lacruzjs.sobreio.generated.resources.icon_furniture
import dev.lacruzjs.sobreio.generated.resources.icon_games
import dev.lacruzjs.sobreio.generated.resources.icon_garden
import dev.lacruzjs.sobreio.generated.resources.icon_gas
import dev.lacruzjs.sobreio.generated.resources.icon_gifts
import dev.lacruzjs.sobreio.generated.resources.icon_groceries
import dev.lacruzjs.sobreio.generated.resources.icon_group
import dev.lacruzjs.sobreio.generated.resources.icon_gym
import dev.lacruzjs.sobreio.generated.resources.icon_hairdresser
import dev.lacruzjs.sobreio.generated.resources.icon_headphones
import dev.lacruzjs.sobreio.generated.resources.icon_heart
import dev.lacruzjs.sobreio.generated.resources.icon_hobbies
import dev.lacruzjs.sobreio.generated.resources.icon_home
import dev.lacruzjs.sobreio.generated.resources.icon_hospital
import dev.lacruzjs.sobreio.generated.resources.icon_hotel
import dev.lacruzjs.sobreio.generated.resources.icon_ice_cream
import dev.lacruzjs.sobreio.generated.resources.icon_insurance
import dev.lacruzjs.sobreio.generated.resources.icon_internet
import dev.lacruzjs.sobreio.generated.resources.icon_kids
import dev.lacruzjs.sobreio.generated.resources.icon_laundry
import dev.lacruzjs.sobreio.generated.resources.icon_loan
import dev.lacruzjs.sobreio.generated.resources.icon_metro
import dev.lacruzjs.sobreio.generated.resources.icon_misc
import dev.lacruzjs.sobreio.generated.resources.icon_mobile_payment
import dev.lacruzjs.sobreio.generated.resources.icon_mortgage
import dev.lacruzjs.sobreio.generated.resources.icon_motorbike
import dev.lacruzjs.sobreio.generated.resources.icon_music
import dev.lacruzjs.sobreio.generated.resources.icon_office_supplies
import dev.lacruzjs.sobreio.generated.resources.icon_parking
import dev.lacruzjs.sobreio.generated.resources.icon_pension
import dev.lacruzjs.sobreio.generated.resources.icon_people
import dev.lacruzjs.sobreio.generated.resources.icon_person
import dev.lacruzjs.sobreio.generated.resources.icon_pet
import dev.lacruzjs.sobreio.generated.resources.icon_pharmacy
import dev.lacruzjs.sobreio.generated.resources.icon_phone
import dev.lacruzjs.sobreio.generated.resources.icon_photography
import dev.lacruzjs.sobreio.generated.resources.icon_pie_chart
import dev.lacruzjs.sobreio.generated.resources.icon_pizza
import dev.lacruzjs.sobreio.generated.resources.icon_pound
import dev.lacruzjs.sobreio.generated.resources.icon_property
import dev.lacruzjs.sobreio.generated.resources.icon_rent
import dev.lacruzjs.sobreio.generated.resources.icon_repairs
import dev.lacruzjs.sobreio.generated.resources.icon_restaurant
import dev.lacruzjs.sobreio.generated.resources.icon_rocket
import dev.lacruzjs.sobreio.generated.resources.icon_roommates
import dev.lacruzjs.sobreio.generated.resources.icon_safe
import dev.lacruzjs.sobreio.generated.resources.icon_salary
import dev.lacruzjs.sobreio.generated.resources.icon_savings
import dev.lacruzjs.sobreio.generated.resources.icon_school
import dev.lacruzjs.sobreio.generated.resources.icon_section_banks
import dev.lacruzjs.sobreio.generated.resources.icon_section_currency
import dev.lacruzjs.sobreio.generated.resources.icon_section_family
import dev.lacruzjs.sobreio.generated.resources.icon_section_finance
import dev.lacruzjs.sobreio.generated.resources.icon_section_food
import dev.lacruzjs.sobreio.generated.resources.icon_section_health
import dev.lacruzjs.sobreio.generated.resources.icon_section_home
import dev.lacruzjs.sobreio.generated.resources.icon_section_leisure
import dev.lacruzjs.sobreio.generated.resources.icon_section_other
import dev.lacruzjs.sobreio.generated.resources.icon_section_people
import dev.lacruzjs.sobreio.generated.resources.icon_section_savings_investment
import dev.lacruzjs.sobreio.generated.resources.icon_section_shopping
import dev.lacruzjs.sobreio.generated.resources.icon_section_transport
import dev.lacruzjs.sobreio.generated.resources.icon_section_work
import dev.lacruzjs.sobreio.generated.resources.icon_section_work_education
import dev.lacruzjs.sobreio.generated.resources.icon_shopping_bag
import dev.lacruzjs.sobreio.generated.resources.icon_ski
import dev.lacruzjs.sobreio.generated.resources.icon_spa
import dev.lacruzjs.sobreio.generated.resources.icon_sport
import dev.lacruzjs.sobreio.generated.resources.icon_star
import dev.lacruzjs.sobreio.generated.resources.icon_store
import dev.lacruzjs.sobreio.generated.resources.icon_subscriptions
import dev.lacruzjs.sobreio.generated.resources.icon_taxes
import dev.lacruzjs.sobreio.generated.resources.icon_taxi
import dev.lacruzjs.sobreio.generated.resources.icon_theater
import dev.lacruzjs.sobreio.generated.resources.icon_therapy
import dev.lacruzjs.sobreio.generated.resources.icon_tolls
import dev.lacruzjs.sobreio.generated.resources.icon_toys
import dev.lacruzjs.sobreio.generated.resources.icon_train
import dev.lacruzjs.sobreio.generated.resources.icon_travel
import dev.lacruzjs.sobreio.generated.resources.icon_travel_card
import dev.lacruzjs.sobreio.generated.resources.icon_trending_up
import dev.lacruzjs.sobreio.generated.resources.icon_trophy
import dev.lacruzjs.sobreio.generated.resources.icon_tv
import dev.lacruzjs.sobreio.generated.resources.icon_villa
import dev.lacruzjs.sobreio.generated.resources.icon_wallet
import dev.lacruzjs.sobreio.generated.resources.icon_water
import dev.lacruzjs.sobreio.generated.resources.icon_web
import dev.lacruzjs.sobreio.generated.resources.icon_wine
import dev.lacruzjs.sobreio.generated.resources.icon_work
import org.jetbrains.compose.resources.StringResource

/** Where an icon is chosen: each entity type has its own catalogue. */
enum class IconCategory { Group, Account, Envelope }

/**
 * One selectable icon. [key] is what the database stores in `icon` (snake_case English); it must never change once
 * released. [label] is the accessible name.
 */
class IconOption(val key: String, val label: StringResource, val vector: ImageVector)

class IconSection(val titleRes: StringResource, val icons: List<IconOption>)

/** The icons offered for groups, accounts and envelopes (Material icons), grouped in titled sections. */
object IconCatalog {

    private val all: Map<String, IconOption> by lazy {
        mapOf(
            "home" to IconOption("home", Res.string.icon_home, Icons.Filled.Home),
            "apartment" to IconOption("apartment", Res.string.icon_apartment, Icons.Filled.Apartment),
            "cottage" to IconOption("cottage", Res.string.icon_cottage, Icons.Filled.Cottage),
            "bed" to IconOption("bed", Res.string.icon_bed, Icons.Filled.Bed),
            "person" to IconOption("person", Res.string.icon_person, Icons.Filled.Person),
            "group" to IconOption("group", Res.string.icon_group, Icons.Filled.Group),
            "family" to IconOption("family", Res.string.icon_family, Icons.Filled.FamilyRestroom),
            "baby" to IconOption("baby", Res.string.icon_baby, Icons.Filled.ChildCare),
            "pet" to IconOption("pet", Res.string.icon_pet, Icons.Filled.Pets),
            "heart" to IconOption("heart", Res.string.icon_heart, Icons.Filled.Favorite),
            "work" to IconOption("work", Res.string.icon_work, Icons.Filled.Work),
            "business" to IconOption("business", Res.string.icon_business, Icons.Filled.Business),
            "store" to IconOption("store", Res.string.icon_store, Icons.Filled.Store),
            "flight" to IconOption("flight", Res.string.icon_flight, Icons.Filled.Flight),
            "beach" to IconOption("beach", Res.string.icon_beach, Icons.Filled.BeachAccess),
            "celebration" to IconOption("celebration", Res.string.icon_celebration, Icons.Filled.Celebration),
            "sport" to IconOption("sport", Res.string.icon_sport, Icons.Filled.SportsSoccer),
            "savings" to IconOption("savings", Res.string.icon_savings, Icons.Filled.Savings),
            "school" to IconOption("school", Res.string.icon_school, Icons.Filled.School),
            "star" to IconOption("star", Res.string.icon_star, Icons.Filled.Star),
            "bank" to IconOption("bank", Res.string.icon_bank, Icons.Filled.AccountBalance),
            "credit_card" to IconOption("credit_card", Res.string.icon_credit_card, Icons.Filled.CreditCard),
            "contactless" to IconOption("contactless", Res.string.icon_contactless, Icons.Filled.Contactless),
            "wallet" to IconOption("wallet", Res.string.icon_wallet, Icons.Filled.AccountBalanceWallet),
            "cash" to IconOption("cash", Res.string.icon_cash, Icons.Filled.Payments),
            "atm" to IconOption("atm", Res.string.icon_atm, Icons.Filled.LocalAtm),
            "chart" to IconOption("chart", Res.string.icon_chart, Icons.AutoMirrored.Filled.ShowChart),
            "trending_up" to IconOption("trending_up", Res.string.icon_trending_up, Icons.AutoMirrored.Filled.TrendingUp),
            "candlestick" to IconOption("candlestick", Res.string.icon_candlestick, Icons.Filled.CandlestickChart),
            "bitcoin" to IconOption("bitcoin", Res.string.icon_bitcoin, Icons.Filled.CurrencyBitcoin),
            "diamond" to IconOption("diamond", Res.string.icon_diamond, Icons.Filled.Diamond),
            "safe" to IconOption("safe", Res.string.icon_safe, Icons.Filled.Lock),
            "phone" to IconOption("phone", Res.string.icon_phone, Icons.Filled.PhoneAndroid),
            "web" to IconOption("web", Res.string.icon_web, Icons.Filled.Language),
            "salary" to IconOption("salary", Res.string.icon_salary, Icons.Filled.Paid),
            "flag" to IconOption("flag", Res.string.icon_flag, Icons.Filled.Flag),
            "property" to IconOption("property", Res.string.icon_property, Icons.Filled.HomeWork),
            "groceries" to IconOption("groceries", Res.string.icon_groceries, Icons.Filled.ShoppingCart),
            "restaurant" to IconOption("restaurant", Res.string.icon_restaurant, Icons.Filled.Restaurant),
            "fast_food" to IconOption("fast_food", Res.string.icon_fast_food, Icons.Filled.Fastfood),
            "coffee" to IconOption("coffee", Res.string.icon_coffee, Icons.Filled.LocalCafe),
            "bar" to IconOption("bar", Res.string.icon_bar, Icons.Filled.LocalBar),
            "electricity" to IconOption("electricity", Res.string.icon_electricity, Icons.Filled.Bolt),
            "water" to IconOption("water", Res.string.icon_water, Icons.Filled.WaterDrop),
            "internet" to IconOption("internet", Res.string.icon_internet, Icons.Filled.Wifi),
            "repairs" to IconOption("repairs", Res.string.icon_repairs, Icons.Filled.Build),
            "car" to IconOption("car", Res.string.icon_car, Icons.Filled.DirectionsCar),
            "fuel" to IconOption("fuel", Res.string.icon_fuel, Icons.Filled.LocalGasStation),
            "bus" to IconOption("bus", Res.string.icon_bus, Icons.Filled.DirectionsBus),
            "train" to IconOption("train", Res.string.icon_train, Icons.Filled.Train),
            "parking" to IconOption("parking", Res.string.icon_parking, Icons.Filled.LocalParking),
            "doctor" to IconOption("doctor", Res.string.icon_doctor, Icons.Filled.MedicalServices),
            "pharmacy" to IconOption("pharmacy", Res.string.icon_pharmacy, Icons.Filled.LocalPharmacy),
            "gym" to IconOption("gym", Res.string.icon_gym, Icons.Filled.FitnessCenter),
            "clothes" to IconOption("clothes", Res.string.icon_clothes, Icons.Filled.Checkroom),
            "hairdresser" to IconOption("hairdresser", Res.string.icon_hairdresser, Icons.Filled.ContentCut),
            "kids" to IconOption("kids", Res.string.icon_kids, Icons.Filled.ChildFriendly),
            "cinema" to IconOption("cinema", Res.string.icon_cinema, Icons.Filled.Movie),
            "games" to IconOption("games", Res.string.icon_games, Icons.Filled.SportsEsports),
            "books" to IconOption("books", Res.string.icon_books, Icons.AutoMirrored.Filled.MenuBook),
            "travel" to IconOption("travel", Res.string.icon_travel, Icons.Filled.Luggage),
            "gifts" to IconOption("gifts", Res.string.icon_gifts, Icons.Filled.CardGiftcard),
            "insurance" to IconOption("insurance", Res.string.icon_insurance, Icons.Filled.Security),
            "bills" to IconOption("bills", Res.string.icon_bills, Icons.Filled.Receipt),
            "subscriptions" to IconOption("subscriptions", Res.string.icon_subscriptions, Icons.Filled.Subscriptions),
            "bakery" to IconOption("bakery", Res.string.icon_bakery, Icons.Filled.BakeryDining),
            "pizza" to IconOption("pizza", Res.string.icon_pizza, Icons.Filled.LocalPizza),
            "ice_cream" to IconOption("ice_cream", Res.string.icon_ice_cream, Icons.Filled.Icecream),
            "delivery" to IconOption("delivery", Res.string.icon_delivery, Icons.Filled.DeliveryDining),
            "wine" to IconOption("wine", Res.string.icon_wine, Icons.Filled.WineBar),
            "rent" to IconOption("rent", Res.string.icon_rent, Icons.Filled.Key),
            "gas" to IconOption("gas", Res.string.icon_gas, Icons.Filled.LocalFireDepartment),
            "cleaning" to IconOption("cleaning", Res.string.icon_cleaning, Icons.Filled.CleaningServices),
            "furniture" to IconOption("furniture", Res.string.icon_furniture, Icons.Filled.Chair),
            "appliances" to IconOption("appliances", Res.string.icon_appliances, Icons.Filled.Kitchen),
            "garden" to IconOption("garden", Res.string.icon_garden, Icons.Filled.Yard),
            "laundry" to IconOption("laundry", Res.string.icon_laundry, Icons.Filled.LocalLaundryService),
            "tv" to IconOption("tv", Res.string.icon_tv, Icons.Filled.Tv),
            "ev_charging" to IconOption("ev_charging", Res.string.icon_ev_charging, Icons.Filled.EvStation),
            "metro" to IconOption("metro", Res.string.icon_metro, Icons.Filled.Subway),
            "taxi" to IconOption("taxi", Res.string.icon_taxi, Icons.Filled.LocalTaxi),
            "bike" to IconOption("bike", Res.string.icon_bike, Icons.Filled.PedalBike),
            "motorbike" to IconOption("motorbike", Res.string.icon_motorbike, Icons.Filled.TwoWheeler),
            "tolls" to IconOption("tolls", Res.string.icon_tolls, Icons.Filled.Toll),
            "car_repair" to IconOption("car_repair", Res.string.icon_car_repair, Icons.Filled.CarRepair),
            "hospital" to IconOption("hospital", Res.string.icon_hospital, Icons.Filled.LocalHospital),
            "therapy" to IconOption("therapy", Res.string.icon_therapy, Icons.Filled.Psychology),
            "spa" to IconOption("spa", Res.string.icon_spa, Icons.Filled.Spa),
            "beauty" to IconOption("beauty", Res.string.icon_beauty, Icons.Filled.Face),
            "toys" to IconOption("toys", Res.string.icon_toys, Icons.Filled.Toys),
            "elderly_care" to IconOption("elderly_care", Res.string.icon_elderly_care, Icons.Filled.ElderlyWoman),
            "shopping_bag" to IconOption("shopping_bag", Res.string.icon_shopping_bag, Icons.Filled.ShoppingBag),
            "electronics" to IconOption("electronics", Res.string.icon_electronics, Icons.Filled.Devices),
            "headphones" to IconOption("headphones", Res.string.icon_headphones, Icons.Filled.Headphones),
            "music" to IconOption("music", Res.string.icon_music, Icons.Filled.MusicNote),
            "theater" to IconOption("theater", Res.string.icon_theater, Icons.Filled.TheaterComedy),
            "hotel" to IconOption("hotel", Res.string.icon_hotel, Icons.Filled.Hotel),
            "ski" to IconOption("ski", Res.string.icon_ski, Icons.Filled.DownhillSkiing),
            "camping" to IconOption("camping", Res.string.icon_camping, Icons.Filled.Forest),
            "hobbies" to IconOption("hobbies", Res.string.icon_hobbies, Icons.Filled.Palette),
            "photography" to IconOption("photography", Res.string.icon_photography, Icons.Filled.PhotoCamera),
            "emergency_fund" to IconOption("emergency_fund", Res.string.icon_emergency_fund, Icons.Filled.Emergency),
            "taxes" to IconOption("taxes", Res.string.icon_taxes, Icons.Filled.RequestQuote),
            "loan" to IconOption("loan", Res.string.icon_loan, Icons.Filled.CreditScore),
            "bank_fees" to IconOption("bank_fees", Res.string.icon_bank_fees, Icons.Filled.Percent),
            "donations" to IconOption("donations", Res.string.icon_donations, Icons.Filled.VolunteerActivism),
            "courses" to IconOption("courses", Res.string.icon_courses, Icons.Filled.Class),
            "office_supplies" to IconOption("office_supplies", Res.string.icon_office_supplies, Icons.Filled.Inventory2),
            "computer" to IconOption("computer", Res.string.icon_computer, Icons.Filled.Computer),
            "birthday" to IconOption("birthday", Res.string.icon_birthday, Icons.Filled.Cake),
            "christmas" to IconOption("christmas", Res.string.icon_christmas, Icons.Filled.AcUnit),
            "misc" to IconOption("misc", Res.string.icon_misc, Icons.Filled.Category),
            "debit_card" to IconOption("debit_card", Res.string.icon_debit_card, Icons.Filled.Payment),
            "mobile_payment" to IconOption("mobile_payment", Res.string.icon_mobile_payment, Icons.Filled.PhonelinkRing),
            "business_account" to IconOption("business_account", Res.string.icon_business_account, Icons.Filled.BusinessCenter),
            "travel_card" to IconOption("travel_card", Res.string.icon_travel_card, Icons.Filled.CardTravel),
            "pie_chart" to IconOption("pie_chart", Res.string.icon_pie_chart, Icons.Filled.PieChart),
            "pension" to IconOption("pension", Res.string.icon_pension, Icons.Filled.Elderly),
            "currency_exchange" to IconOption("currency_exchange", Res.string.icon_currency_exchange, Icons.Filled.CurrencyExchange),
            "euro" to IconOption("euro", Res.string.icon_euro, Icons.Filled.Euro),
            "dollar" to IconOption("dollar", Res.string.icon_dollar, Icons.Filled.AttachMoney),
            "pound" to IconOption("pound", Res.string.icon_pound, Icons.Filled.CurrencyPound),
            "mortgage" to IconOption("mortgage", Res.string.icon_mortgage, Icons.Filled.RealEstateAgent),
            "villa" to IconOption("villa", Res.string.icon_villa, Icons.Filled.Villa),
            "people" to IconOption("people", Res.string.icon_people, Icons.Filled.People),
            "friends" to IconOption("friends", Res.string.icon_friends, Icons.Filled.Groups),
            "roommates" to IconOption("roommates", Res.string.icon_roommates, Icons.Filled.MeetingRoom),
            "couple" to IconOption("couple", Res.string.icon_couple, Icons.Filled.Diversity1),
            "freelance" to IconOption("freelance", Res.string.icon_freelance, Icons.Filled.LaptopMac),
            "trophy" to IconOption("trophy", Res.string.icon_trophy, Icons.Filled.EmojiEvents),
            "rocket" to IconOption("rocket", Res.string.icon_rocket, Icons.Filled.RocketLaunch),
        )
    }

    private fun section(title: StringResource, vararg keys: String) =
        IconSection(title, keys.map { all.getValue(it) })

    private val groupSections: List<IconSection> by lazy {
        listOf(
            section(Res.string.icon_section_home, "home", "apartment", "cottage", "villa", "bed"),
            section(Res.string.icon_section_people, "person", "couple", "family", "baby", "group", "people", "friends", "roommates", "pet", "heart"),
            section(Res.string.icon_section_work, "work", "business", "store", "freelance"),
            section(Res.string.icon_section_leisure, "flight", "travel", "beach", "camping", "ski", "sport", "music", "games", "celebration"),
            section(Res.string.icon_section_other, "savings", "school", "star", "flag", "trophy", "rocket"),
        )
    }

    private val accountSections: List<IconSection> by lazy {
        listOf(
            section(Res.string.icon_section_banks, "bank", "credit_card", "debit_card", "contactless", "mobile_payment", "wallet", "cash", "atm", "business_account", "travel_card"),
            section(Res.string.icon_section_savings_investment, "savings", "chart", "trending_up", "candlestick", "pie_chart", "bitcoin", "diamond", "pension", "safe"),
            section(Res.string.icon_section_currency, "currency_exchange", "euro", "dollar", "pound"),
            section(Res.string.icon_section_other, "phone", "web", "salary", "person", "group", "flag", "property", "mortgage", "loan"),
        )
    }

    private val envelopeSections: List<IconSection> by lazy {
        listOf(
            section(Res.string.icon_section_food, "groceries", "restaurant", "fast_food", "coffee", "bar", "wine", "bakery", "pizza", "ice_cream", "delivery"),
            section(Res.string.icon_section_home, "home", "rent", "electricity", "gas", "water", "internet", "phone", "tv", "cleaning", "laundry", "furniture", "appliances", "garden", "repairs"),
            section(Res.string.icon_section_transport, "car", "fuel", "ev_charging", "bus", "train", "metro", "taxi", "bike", "motorbike", "parking", "tolls", "car_repair"),
            section(Res.string.icon_section_health, "doctor", "hospital", "pharmacy", "therapy", "gym", "spa", "hairdresser", "beauty"),
            section(Res.string.icon_section_family, "kids", "baby", "family", "school", "toys", "pet", "elderly_care"),
            section(Res.string.icon_section_shopping, "clothes", "shopping_bag", "electronics", "headphones", "gifts"),
            section(Res.string.icon_section_leisure, "cinema", "theater", "music", "games", "books", "sport", "travel", "flight", "hotel", "beach", "ski", "camping", "hobbies", "photography"),
            section(Res.string.icon_section_finance, "savings", "emergency_fund", "insurance", "bills", "subscriptions", "taxes", "loan", "bank_fees", "donations", "trending_up"),
            section(Res.string.icon_section_work_education, "work", "courses", "office_supplies", "computer"),
            section(Res.string.icon_section_other, "celebration", "birthday", "christmas", "heart", "star", "misc"),
        )
    }

    fun sections(category: IconCategory): List<IconSection> = when (category) {
        IconCategory.Group -> groupSections
        IconCategory.Account -> accountSections
        IconCategory.Envelope -> envelopeSections
    }

    /** The icon stored under [key] in [category], or null when there is none or the key is unknown (newer app version). */
    fun iconFor(category: IconCategory, key: String?): IconOption? {
        if (key == null) return null
        return sections(category).firstNotNullOfOrNull { section -> section.icons.firstOrNull { it.key == key } }
    }
}

/** The vector for [key], or null for no icon or an unknown key. */
fun iconFor(category: IconCategory, key: String?): ImageVector? = IconCatalog.iconFor(category, key)?.vector

package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.icon_apartment
import dev.lacruzjs.sobreio.generated.resources.icon_atm
import dev.lacruzjs.sobreio.generated.resources.icon_baby
import dev.lacruzjs.sobreio.generated.resources.icon_bank
import dev.lacruzjs.sobreio.generated.resources.icon_bar
import dev.lacruzjs.sobreio.generated.resources.icon_beach
import dev.lacruzjs.sobreio.generated.resources.icon_bed
import dev.lacruzjs.sobreio.generated.resources.icon_bills
import dev.lacruzjs.sobreio.generated.resources.icon_bitcoin
import dev.lacruzjs.sobreio.generated.resources.icon_books
import dev.lacruzjs.sobreio.generated.resources.icon_bus
import dev.lacruzjs.sobreio.generated.resources.icon_business
import dev.lacruzjs.sobreio.generated.resources.icon_candlestick
import dev.lacruzjs.sobreio.generated.resources.icon_car
import dev.lacruzjs.sobreio.generated.resources.icon_cash
import dev.lacruzjs.sobreio.generated.resources.icon_celebration
import dev.lacruzjs.sobreio.generated.resources.icon_chart
import dev.lacruzjs.sobreio.generated.resources.icon_cinema
import dev.lacruzjs.sobreio.generated.resources.icon_clothes
import dev.lacruzjs.sobreio.generated.resources.icon_coffee
import dev.lacruzjs.sobreio.generated.resources.icon_contactless
import dev.lacruzjs.sobreio.generated.resources.icon_cottage
import dev.lacruzjs.sobreio.generated.resources.icon_credit_card
import dev.lacruzjs.sobreio.generated.resources.icon_diamond
import dev.lacruzjs.sobreio.generated.resources.icon_doctor
import dev.lacruzjs.sobreio.generated.resources.icon_electricity
import dev.lacruzjs.sobreio.generated.resources.icon_family
import dev.lacruzjs.sobreio.generated.resources.icon_fast_food
import dev.lacruzjs.sobreio.generated.resources.icon_flag
import dev.lacruzjs.sobreio.generated.resources.icon_flight
import dev.lacruzjs.sobreio.generated.resources.icon_fuel
import dev.lacruzjs.sobreio.generated.resources.icon_games
import dev.lacruzjs.sobreio.generated.resources.icon_gifts
import dev.lacruzjs.sobreio.generated.resources.icon_groceries
import dev.lacruzjs.sobreio.generated.resources.icon_group
import dev.lacruzjs.sobreio.generated.resources.icon_gym
import dev.lacruzjs.sobreio.generated.resources.icon_hairdresser
import dev.lacruzjs.sobreio.generated.resources.icon_heart
import dev.lacruzjs.sobreio.generated.resources.icon_home
import dev.lacruzjs.sobreio.generated.resources.icon_insurance
import dev.lacruzjs.sobreio.generated.resources.icon_internet
import dev.lacruzjs.sobreio.generated.resources.icon_kids
import dev.lacruzjs.sobreio.generated.resources.icon_parking
import dev.lacruzjs.sobreio.generated.resources.icon_person
import dev.lacruzjs.sobreio.generated.resources.icon_pet
import dev.lacruzjs.sobreio.generated.resources.icon_pharmacy
import dev.lacruzjs.sobreio.generated.resources.icon_phone
import dev.lacruzjs.sobreio.generated.resources.icon_property
import dev.lacruzjs.sobreio.generated.resources.icon_repairs
import dev.lacruzjs.sobreio.generated.resources.icon_restaurant
import dev.lacruzjs.sobreio.generated.resources.icon_safe
import dev.lacruzjs.sobreio.generated.resources.icon_salary
import dev.lacruzjs.sobreio.generated.resources.icon_savings
import dev.lacruzjs.sobreio.generated.resources.icon_school
import dev.lacruzjs.sobreio.generated.resources.icon_section_banks
import dev.lacruzjs.sobreio.generated.resources.icon_section_finance
import dev.lacruzjs.sobreio.generated.resources.icon_section_food
import dev.lacruzjs.sobreio.generated.resources.icon_section_health
import dev.lacruzjs.sobreio.generated.resources.icon_section_home
import dev.lacruzjs.sobreio.generated.resources.icon_section_leisure
import dev.lacruzjs.sobreio.generated.resources.icon_section_other
import dev.lacruzjs.sobreio.generated.resources.icon_section_people
import dev.lacruzjs.sobreio.generated.resources.icon_section_personal
import dev.lacruzjs.sobreio.generated.resources.icon_section_savings_investment
import dev.lacruzjs.sobreio.generated.resources.icon_section_transport
import dev.lacruzjs.sobreio.generated.resources.icon_section_work
import dev.lacruzjs.sobreio.generated.resources.icon_sport
import dev.lacruzjs.sobreio.generated.resources.icon_star
import dev.lacruzjs.sobreio.generated.resources.icon_store
import dev.lacruzjs.sobreio.generated.resources.icon_subscriptions
import dev.lacruzjs.sobreio.generated.resources.icon_train
import dev.lacruzjs.sobreio.generated.resources.icon_travel
import dev.lacruzjs.sobreio.generated.resources.icon_trending_up
import dev.lacruzjs.sobreio.generated.resources.icon_wallet
import dev.lacruzjs.sobreio.generated.resources.icon_water
import dev.lacruzjs.sobreio.generated.resources.icon_web
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
            "chart" to IconOption("chart", Res.string.icon_chart, Icons.Filled.ShowChart),
            "trending_up" to IconOption("trending_up", Res.string.icon_trending_up, Icons.Filled.TrendingUp),
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
            "books" to IconOption("books", Res.string.icon_books, Icons.Filled.MenuBook),
            "travel" to IconOption("travel", Res.string.icon_travel, Icons.Filled.Luggage),
            "gifts" to IconOption("gifts", Res.string.icon_gifts, Icons.Filled.CardGiftcard),
            "insurance" to IconOption("insurance", Res.string.icon_insurance, Icons.Filled.Security),
            "bills" to IconOption("bills", Res.string.icon_bills, Icons.Filled.Receipt),
            "subscriptions" to IconOption("subscriptions", Res.string.icon_subscriptions, Icons.Filled.Subscriptions),
        )
    }

    private fun section(title: StringResource, vararg keys: String) =
        IconSection(title, keys.map { all.getValue(it) })

    private val groupSections: List<IconSection> by lazy {
        listOf(
            section(Res.string.icon_section_home, "home", "apartment", "cottage", "bed"),
            section(Res.string.icon_section_people, "person", "group", "family", "baby", "pet", "heart"),
            section(Res.string.icon_section_work, "work", "business", "store"),
            section(Res.string.icon_section_leisure, "flight", "beach", "celebration", "sport"),
            section(Res.string.icon_section_other, "savings", "school", "star"),
        )
    }

    private val accountSections: List<IconSection> by lazy {
        listOf(
            section(Res.string.icon_section_banks, "bank", "credit_card", "contactless", "wallet", "cash", "atm"),
            section(Res.string.icon_section_savings_investment, "savings", "chart", "trending_up", "candlestick", "bitcoin", "diamond", "safe"),
            section(Res.string.icon_section_other, "phone", "web", "salary", "person", "group", "flag", "property"),
        )
    }

    private val envelopeSections: List<IconSection> by lazy {
        listOf(
            section(Res.string.icon_section_food, "groceries", "restaurant", "fast_food", "coffee", "bar"),
            section(Res.string.icon_section_home, "home", "electricity", "water", "internet", "repairs"),
            section(Res.string.icon_section_transport, "car", "fuel", "bus", "train", "parking"),
            section(Res.string.icon_section_health, "doctor", "pharmacy", "gym"),
            section(Res.string.icon_section_personal, "clothes", "hairdresser", "school", "kids", "pet"),
            section(Res.string.icon_section_leisure, "cinema", "games", "books", "travel", "gifts"),
            section(Res.string.icon_section_finance, "savings", "insurance", "bills", "subscriptions"),
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

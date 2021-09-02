@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.slack.api:slack-api-client:1.11.0")

import com.slack.api.Slack
import java.io.File

/**
 * The changelog will be following the template below 👇 to ensure consistency and have a common language among everyone.
 *
 * Following is description of what each section means, what it will contain and its emoji 😛
 * ✨ Changes section will include product-driven tickets
 * 🤖 Tech changes section will include tickets from the Android Chapter board
 * 🐛 Bug fixes section will includes bug tickets
 * 📈 Instrumentation section will include Analytics tickets
 * 🏳 A/B test will include tickets related to an A/B test when they are released for the first time
 * 🏴 Included but not visible section will include all tickets that are behind a feature flag, or disabled for the end use
 *
 * Example:
 * ✨ Update app colors
 * 🤖 Improve app performance
 * 🐛 Fix app crashing on launch
 * 📈 Send event on app launch
 * 🏳 Implement paywall for content
 * 🏴 CBC ticket
 */

//region Configuration
private val changeEmoji = "✨"
private val techChangesEmoji = "🤖"
private val bugEmoji = "🐛"
private val instrumentationEmoji = "📈"
private val includedButNotVisibleEmoji = "🏴"
//endregion

//region constant
private val issueKey = "Issue key"
private val issueType = "Issue Type"
private val summary = "Summary"
private val changes = "Changes"
private val techChanges = "Tech changes"
private val bugFixes = "Bug fixes"
private val instrumentation = "Instrumentation"
private val labels = "Labels"

//argument handler
main()

fun main() {
    val platform = args.getOrNull(2)?.toPlatform() ?: error("Missing platform type, it should be either Android or iOS")
    val path = args.getOrNull(0) ?: error("Missing path to the source CSV file")
    val webhookPath = args.getOrNull(1) ?: error("Missing path to the source CSV file")
    val releaseNotes = mutableListOf<String>()

    // 0. Get list of Urls
    val webhookUrls = generateUrls(webhookPath).filter { it.platform == platform.toString() }

    // 1. Get list of tickets
    val tickets = generateTickets(path, platform)

    // 2. Print all changes
    tickets
        .filter { it.ticketType.isChange() }
        .ifNotEmpty { releaseNotes.add(changes) }
        .map { releaseNotes.add("$changeEmoji ${it.key} \t ${it.summary}") }

    // 3. Print all tech changes
    tickets
        .filter { it.ticketType is TicketType.Chapter }
        .ifNotEmpty { releaseNotes.add(techChanges) }
        .map { releaseNotes.add("$techChangesEmoji ${it.key} \t ${it.summary}") }

    // 4. Print all bug fixes
    tickets
        .filter { it.ticketType is TicketType.Bug }
        .ifNotEmpty { releaseNotes.add(bugFixes) }
        .map { releaseNotes.add("$bugEmoji ${it.key} \t ${it.summary}") }

    // 5. Print all instrumentation
    tickets
        .filter { it.ticketType is TicketType.Analytics }
        .ifNotEmpty { releaseNotes.add(instrumentation) }
        .map { releaseNotes.add("$instrumentationEmoji ${it.key} \t ${it.summary}") }

    val result = releaseNotes.toList().joinToString(separator = "\n")
    println(result)

    //6. Ask question for announcement
    askQuestion(result, false, webhookUrls,platform)
}

fun resultResponse(result: String, webhookUrls: List<Url>, platform: Platform) {
    when (readLine().toString().lowercase()) {
        "y" -> triggerAnnouncement(result, webhookUrls,platform)
        "n" -> println("Ciao!")//terminate
        else -> askQuestion(result, true, webhookUrls, platform)

    }
}

fun askQuestion(
    result: String,
    isInputWrong: Boolean = false,
    webhookUrls: List<Url>,
    platform: Platform
) {
    when {
        isInputWrong -> println("Please enter a correct input. Press Y/N")
        else -> println("Captain, do you want me to announce to fellow Cluebies on your behalf? Press Y/N")
    }
    resultResponse(result, webhookUrls,platform)
}
//region Data structure

/**
 * Describes the representation
 * of a "Jira ticket"
 */
data class Ticket(
    val key: String,
    val summary: String,
    val ticketType: TicketType
)

/**
 * Describes the representation
 * of a "Webhook Url"
 */
data class Url(
    val url: String,
    val platform: String
)

/**
 * Describes various types of tickets
 */
sealed class TicketType {
    object Analytics : TicketType()
    object Story : TicketType()
    object SubTask : TicketType()
    object Improvement : TicketType()
    object Bug : TicketType()
    object Chapter : TicketType()
    data class Unknown(val type: String) : TicketType()

    /**
     * Returns if the ticket type is a "change"
     * which can either be a story, subtask or
     * an improvement.
     */
    fun isChange(): Boolean {
        return when (this) {
            Story -> true
            SubTask -> true
            Improvement -> true
            else -> false
        }
    }
}

/**
 * Maps string to a [TicketType]
 */
fun String.toTicketType(platform: Platform, labels: List<String>): TicketType {
    return when (this) {
        "Analytics:${platform}" -> TicketType.Analytics
        "Story:${platform}" -> when {
            labels.toString().contains(platform.toString(), true) -> TicketType.Chapter
            else -> TicketType.Story
        }
        "Sub-Task:${platform}" -> TicketType.SubTask
        "Improvement:${platform}" -> TicketType.Improvement
        "Bug:${platform}" -> TicketType.Bug
        else -> TicketType.Unknown(this)
    }
}

/**
 * Describes type of the platform
 */
sealed class Platform {
    object Android : Platform() {
        override fun toString(): String = "android"
    }

    object iOS : Platform() {
        override fun toString(): String = "ios"
    }
}

/**
 * Maps string to a [Platform] type.
 */
fun String.toPlatform(): Platform {
    return when (lowercase()) {
        "android" -> Platform.Android
        "ios" -> Platform.iOS
        else -> error("The platform type needs to be either Android or iOS")
    }
}

//endregion

//region Utilities

/**
 * Execute the [function] if the list is not empty
 * and return the list for chaining.
 */
fun <E> List<E>.ifNotEmpty(function: () -> Unit): List<E> {
    if (isNotEmpty()) function()
    return this
}

/**
 * Reads the CSV file and applies the [processor] and outputs
 * a list of processed items.
 *
 * Do not use this function for huge files, since it's a limitation
 * of [readLines]
 */
fun <T> File.process(processor: (Map<String, List<String>>) -> T): List<T> {
    val bom = "\uFEFF"
    val header = readLines().firstOrNull()
        ?.replace(bom, "")
        ?.split(",")
        ?: throw Exception("This file does not contain a valid header")

    return readLines()
        .asSequence()
        .drop(1)
        .map { it.split(",") }
        .map { header.zip(it).toDublicateMap() }
        .map(processor)
        .toList()
}
//endregion

fun <K, V> Iterable<Pair<K, V>>.toDublicateMap(): Map<K, List<V>> {
    val map = mutableMapOf<K, List<V>>()
    forEach { (key, value) ->
        val containsKey = map.containsKey(key)
        if (containsKey) {
            val existingValue = map.getValue(key)
            map[key] = existingValue + value
        } else {
            map[key] = listOf(value)
        }
    }
    return map
}

fun triggerAnnouncement(result: String, webhookUrls: List<Url>, platform: Platform) {
    println("Great! Let us start the announcement")

    //0. Ask for captain and co-captain name along with the version
    println("Who is the captain for the release?")
    val captain = readLine().toString()
    println("Great! Now, who would be your co-captain?")
    val cocaptain = readLine().toString()
    println("And lastly, what is the version we are releasing?")
    val releaseVersion = readLine().toString()

    val announcementHeader =
        "Hey, for this release in $platform, $captain is the release captain and $cocaptain  is co-captain. Here are the changes in version $releaseVersion"
    val output = "$announcementHeader\n\n$result"

    //slack announcement
    val payload = "{\"text\":\"$output\"}"
    webhookUrls.forEach {
        Slack.getInstance().send(it.url, payload)
    }
}

fun generateUrls(webhookPath: String): List<Url> {
    return File(webhookPath)
        .process { urls ->
            Url(
                url = urls["URL"]?.first() ?: return@process null,
                platform = urls["platform"]?.first() ?: return@process null
            )
        }
        .mapNotNull { it }
}

fun generateTickets(path: String, platform: Platform): List<Ticket> {
    return File(path)
        .process { row ->
            Ticket(
                key = (row[issueKey]?.first() ?: return@process null)
                    .padEnd(15),
                summary = (row[summary]?.first() ?: return@process null)
                    .replace("[$platform]", "", true)
                    .replace(" - $platform", "", true)
                    .trim(),
                ticketType = row[issueType]?.first().orEmpty().toTicketType(platform, row[labels] ?: emptyList())
            )
        }
        .mapNotNull { it }
}

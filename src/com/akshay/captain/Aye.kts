package com.akshay.captain

import java.io.File

main()

fun main() {
    val path = args.getOrNull(0) ?: error("Missing path to the source CSV file")
    val platform = args.getOrNull(1)?.toPlatform() ?: error("Missing platform type, it should be either Android or iOS")

    // 1. Get list of tickets
    val tickets = File(path)
        .process { row ->
            Ticket(
                key = row["Issue key"] ?: return@process null,
                summary = (row["Summary"] ?: return@process null)
                    .replace("[$platform]", "", true)
                    .replace(" - $platform", "", true)
                    .trim(),
                ticketType = row["Issue Type"].orEmpty().toTicketType(platform)
            )
        }
        .mapNotNull { it }


    // 2. Print all changes
    tickets
        .filter { it.ticketType.isChange() }
        .ifNotEmpty { println("Changes") }
        .map { println("✨ ${it.key} \t ${it.summary}") }
        .ifNotEmpty { println() }

    // 3. Print all bug fixes
    tickets
        .filter { it.ticketType is TicketType.Bug }
        .ifNotEmpty { println("Bug fixes") }
        .map { println("🐛 ${it.key} \t ${it.summary}") }
        .ifNotEmpty { println() }

    // 3. Print all instrumentation
    tickets
        .filter { it.ticketType is TicketType.Analytics }
        .ifNotEmpty {  println("Instrumentation") }
        .map { println("📈 ${it.key} \t ${it.summary}") }
        .ifNotEmpty { println() }
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
 * Describes various types of tickets
 */
sealed class TicketType {
    object Analytics : TicketType()
    object Story : TicketType()
    object SubTask : TicketType()
    object Improvement : TicketType()
    object Bug : TicketType()
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
fun String.toTicketType(platform: Platform): TicketType {
    return when (this) {
        "Analytics:${platform}" -> TicketType.Analytics
        "Story:${platform}" -> TicketType.Story
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
fun String.toPlatform(): Platform{
    return when (toLowerCase()) {
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
fun <T> File.process(processor: (Map<String, String>) -> T): List<T> {
    val bom = "\uFEFF"
    val header = readLines().firstOrNull()
        ?.replace(bom, "")
        ?.split(",")
        ?: throw Exception("This file does not contain a valid header")

    return readLines()
        .drop(1)
        .map { it.split(",") }
        .map { header.zip(it).toMap() }
        .map(processor)
        .toList()
}
//endregion
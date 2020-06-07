#!/usr/bin/env kscript
@file:DependsOn("info.picocli:picocli:4.2.0")

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.util.concurrent.Callable
import java.time.*
import java.time.format.*
import java.util.concurrent.*

@Command(name = "create-blogpost", mixinStandardHelpOptions = true, version = ["1.0"])
class GenerateSnippets : Callable<Int> {

    @Option(names = ["-t", "--title"], description = ["Title of the blogpost"])
    private var title: String = ""

    @Option(names = ["-i", "--image"], paramLabel = "IMAGE", description = ["The input .yml file to read from"])
    private var image: String = ""

    override fun call(): Int {
        info("🖋🖋🖋 create-blogpost ✒️✒️✒️️️️", "")
        info("Welcome to create-blogpost", "👋")
        info("Creating your blogpost...", "")

        val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val id = title.toLowerCase().replace(" ", "-")
        val filename = "./_posts/$date-$id.md"
        val headerFilename = "./assets/images/posts/header-$id.jpg"
        val teaserFilename = "./assets/images/posts/teaser-$id.jpg"

        // language=YAML
        val content = """
            ---
            title: "$title"
            categories: "Android"

            excerpt: "TODO"

            header:
                image: "$headerFilename"
                teaser: "$teaserFilename"
                caption: "Stockholm - Sweden"
            ---
        """.trimIndent()

        File(filename).writeText(content)

        if (image.isNotBlank()) {
            info("Resizing your image...", "")
            "mogrify -resize 1920x -quality 85 -write $headerFilename $image".runCommand()
            "mogrify -resize 600x -quality 85 -write $teaserFilename $image".runCommand()
        } else {
            warn("No image provided, skipping resizing.")
        }

        info("Blogpost title: $title", "")
        info("Blogpost id: $id", "")
        info("Blogpost file: $filename", "")
        info("Header file: $headerFilename", "")
        info("Teaser file: $teaserFilename", "")

        succ("Blogpost created successfully!")
        return 0
    }


    /*
     * DEBUG Prints function
     ******************************************************************/

    fun error(message: String, throwable: Throwable? = null, statusCode: Int = 1): Nothing {
        System.err.println("❌\t${Colors.ANSI_RED}$message${Colors.ANSI_RESET}")
        throwable?.let {
            System.err.print(Colors.ANSI_RED)
            it.printStackTrace()
            System.err.print(Colors.ANSI_RESET)

        }
        System.exit(statusCode)
        throw Error()
    }

    fun warn(message: String) {
        System.out.println("⚠️\t${Colors.ANSI_YELLOW}$message${Colors.ANSI_RESET}")
    }

    fun succ(message: String) {
        System.out.println("✅\t${Colors.ANSI_GREEN}$message${Colors.ANSI_RESET}")
    }

    fun info(message: String, emoji: String = "ℹ️") {
        System.out.println("$emoji\t$message")
    }

    fun String.runCommand(
            workingDir: File = File("."),
            timeoutAmount: Long = 60
    ): String? = try {
        ProcessBuilder(split("\\s".toRegex()))
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start().apply { waitFor(timeoutAmount, TimeUnit.SECONDS) }
                .inputStream.bufferedReader().readText()
    } catch (e: java.io.IOException) {
        e.printStackTrace()
        null
    }
}

CommandLine(GenerateSnippets()).execute(*args)

/*
 * ASCII Color
 ******************************************************************/

object Colors {
    val ANSI_RESET = "\u001B[0m"
    val ANSI_BLACK = "\u001B[30m"
    val ANSI_RED = "\u001B[31m"
    val ANSI_GREEN = "\u001B[32m"
    val ANSI_YELLOW = "\u001B[33m"
    val ANSI_BLUE = "\u001B[34m"
    val ANSI_PURPLE = "\u001B[35m"
    val ANSI_CYAN = "\u001B[36m"
    val ANSI_WHITE = "\u001B[37m"
}
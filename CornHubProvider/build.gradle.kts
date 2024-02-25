// use an integer for version numbers
version = 8


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "Persian Worlds"
    authors = listOf("N4S3R", "Stormunblessed", "Jace", "KillerDogeEmpire", "Hexated", "Coxju")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1 // will be 3 if unspecified

    // List of video source types. Users are able to filter for extensions in a given category.
    // You can find a list of avaliable types here:
    // https://recloudstream.github.io/cloudstream/html/app/com.lagradost.cloudstream3/-tv-type/index.html
    tvTypes = listOf("NSFW")

    iconUrl = "https://raw.githubusercontent.com/NoMeatNo/Peravo2/master/logos/peravo2.png"

    language = "en"
}

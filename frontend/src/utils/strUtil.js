export const toTypeLabel = (str) => {
    switch (str) {
        case "all":
            return "All";
        case "tv":
            return "TV";
        case "ona":
            return "ONA";
        case "movie":
            return "Movie";
        case "tv_special":
            return "TV Special";
        default:
            return str;
    }
};

export const toSourceLabel = (str) => {
    if (!str) return "";
    const newStr = str.replace(/_/g, " ");
    return newStr.charAt(0).toUpperCase() + newStr.slice(1);
};

export const toAirStatusLabel = (str) => {
    switch (str) {
        case "all":
            return "All";
        case "currently_airing":
            return "Airing";
        case "finished_airing":
            return "Ended";
        default:
            return str;
    }
};

export const toEpisodeLabel = (str) => {
    if (str === 0) {
        return "?";
    }
    return str;
};

export const toScoreLabel = (str) => {
    return str?.toFixed(2) || "N/A";
};

export const toDateLabel = (str) => {
    if (str == null) {
        return "Unknown";
    }
    return str.split("T")[0];
};
import apiClient from "./apiClient";

const AnimeApi = {
    getByYearAndSeason: async (year, season) => {
        try {
            const response = await apiClient.get("/anime", {
                params: {year, season},
                headers: {
                    Accept: "application/json",
                },
            });
            console.log(response)
            return response.data;
        } catch (error) {
            console.error("Error finding animes by year and season:", error);
            throw error;
        }
    },
};

export default AnimeApi;
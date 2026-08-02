import {useEffect, useMemo, useState} from "react";
import AnimeApi from "../../../../api/AnimeApi";

export const useAnimeData = ({year, season, animeList, selected, sortBy, filterBy, page, pageSize}) => {
    const [animes, setAnimes] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!selected) return;

        let isMounted = true;

        if (year != null && season != null) {
            (async () => {
                setLoading(true);
                setError(null);
                try {
                    const data = await AnimeApi.getByYearAndSeason(year, season);
                    if (isMounted) setAnimes(data);
                } catch (err) {
                    if (isMounted) setError("Failed to fetch season anime");
                } finally {
                    if (isMounted) setLoading(false);
                }
            })();
        } else if (animeList != null) {
            setAnimes(animeList);
        }

        return () => {
            isMounted = false;
        };
    }, [year, season, animeList, selected]);

    const sortedAndFilteredAnimes = useMemo(() => {
        let result = [...animes];

        if (filterBy && (filterBy.type !== "all" || filterBy.airStatus !== "all")) {
            result = result.filter(
                (anime) =>
                    (filterBy.type === "all" || anime.type === filterBy.type) &&
                    (filterBy.airStatus === "all" || anime.airStatus === filterBy.airStatus)
            );
        }

        if (sortBy) {
            const criterion = sortBy === "votes" ? "scoringCount" : sortBy;

            result.sort((a, b) => {
                const aVal = a[criterion] ?? null;
                const bVal = b[criterion] ?? null;

                if (aVal === null && bVal !== null) return 1;
                if (bVal === null && aVal !== null) return -1;

                if (["score", "members", "scoringCount"].includes(criterion)) {
                    return bVal - aVal;
                }
                if (["rank", "popularity"].includes(criterion)) {
                    return aVal - bVal;
                }
                if (criterion === "startDate") {
                    return new Date(aVal) - new Date(bVal);
                }
                return 0;
            });
        }

        return result;
    }, [animes, sortBy, filterBy]);

    const currentPageAnimes = useMemo(() => {
        const startIndex = (page - 1) * pageSize;
        return sortedAndFilteredAnimes.slice(startIndex, startIndex + pageSize);
    }, [sortedAndFilteredAnimes, page, pageSize]);

    return {
        currentPageAnimes,
        totalCount: sortedAndFilteredAnimes.length,
        loading,
        error,
    };
};
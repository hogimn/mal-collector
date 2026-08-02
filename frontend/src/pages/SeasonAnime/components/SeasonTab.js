import {useCallback, useEffect, useMemo, useState} from "react";
import {useNavigate} from "react-router-dom";
import AnimeList from "./AnimeList";
import {
    getCurrentSeason,
    getCurrentSeasonYear,
    getNextSeasonFromSeason,
    getNextSeasonYearFromYearAndSeason,
    getPreviousSeasonFromSeason,
    getPreviousSeasonYearFromYearAndSeason,
} from "../../../utils/dateUtil";
import CommonSelect from "../../../components/base/CommonSelect";
import {toAirStatusLabel, toTypeLabel} from "../../../utils/strUtil";
import {CustomTabs, SelectWrapper, StyledSeasonTab} from "./styles/SeasonTab.style";

const LOCAL_STORAGE_KEY = "seasonFilters";
const PAGE_SIZE = 12;

const getInitialFilters = () => {
    const saved = localStorage.getItem(LOCAL_STORAGE_KEY);
    if (saved) {
        try {
            const parsed = JSON.parse(saved);
            return {
                sortBy: parsed.sortBy || "score",
                filterBy: parsed.filterBy || {type: "tv", airStatus: "all"},
            };
        } catch {

        }
    }
    return {
        sortBy: "score",
        filterBy: {type: "tv", airStatus: "all"},
    };
};

const SeasonTab = ({season, year}) => {
    const navigate = useNavigate();

    const [activeTab, setActiveTab] = useState("4");
    const [page, setPage] = useState(1);

    const [filters, setFilters] = useState(getInitialFilters);
    const {sortBy, filterBy} = filters;

    const seasonData = useMemo(() => {
        let baseSeason = season;
        let baseYear = year;

        if (baseSeason == null || baseYear == null) {
            baseSeason = getCurrentSeason();
            baseYear = getCurrentSeasonYear();
        } else {
            baseYear = parseInt(baseYear, 10);
        }

        const seasons = [{season: baseSeason, year: baseYear}];

        let currSeason = baseSeason;
        let currYear = baseYear;
        for (let i = 0; i < 3; i++) {
            const prevS = getPreviousSeasonFromSeason(currSeason);
            const prevY = parseInt(getPreviousSeasonYearFromYearAndSeason(currYear, currSeason), 10);
            seasons.unshift({season: prevS, year: prevY});
            currSeason = prevS;
            currYear = prevY;
        }

        return seasons;
    }, [season, year]);

    const handleTabChange = useCallback(
        (key) => {
            const targetIndex = parseInt(key, 10) - 1;

            if (targetIndex >= 0 && targetIndex < seasonData.length) {
                const target = seasonData[targetIndex];
                navigate(`/season-anime?year=${target.year}&season=${target.season}`);
                setPage(1);
            } else if (key === "prev") {
                const firstTarget = seasonData[0];
                const nextSeason = getPreviousSeasonFromSeason(firstTarget.season);
                const nextYear = getPreviousSeasonYearFromYearAndSeason(
                    firstTarget.year,
                    firstTarget.season
                );
                navigate(`/season-anime?year=${nextYear}&season=${nextSeason}`);
                setPage(1);
            } else if (key === "next") {
                const lastTarget = seasonData[seasonData.length - 1];
                const nextSeason = getNextSeasonFromSeason(lastTarget.season);
                const nextYear = getNextSeasonYearFromYearAndSeason(
                    lastTarget.year,
                    lastTarget.season
                );
                navigate(`/season-anime?year=${nextYear}&season=${nextSeason}`);
                setPage(1);
            } else if (key === "current") {
                const currentSeason = getCurrentSeason();
                const currentYear = getCurrentSeasonYear();
                navigate(`/season-anime?year=${currentYear}&season=${currentSeason}`);
                setPage(1);
            } else if (key === "archive") {
                navigate("/season-archive");
            } else {
                setActiveTab(key);
                setPage(1);
            }
        },
        [navigate, seasonData]
    );

    const tabs = useMemo(() => {
        return [
            {key: "prev", label: "...", content: null},
            ...seasonData.map(({season: itemSeason, year: itemYear}, index) => ({
                key: `${index + 1}`,
                label: `${itemSeason.charAt(0).toUpperCase() + itemSeason.slice(1)} ${itemYear}`,
                content: (
                    <AnimeList
                        year={itemYear}
                        season={itemSeason}
                        sortBy={sortBy}
                        filterBy={filterBy}
                        page={page}
                        setPage={setPage}
                        pageSize={PAGE_SIZE}
                        selected={activeTab === `${index + 1}`}
                    />
                ),
            })),
            {key: "next", label: "...", content: null},
            {key: "current", label: "Current", content: null},
            {key: "archive", label: "Archive", content: null},
        ];
    }, [seasonData, sortBy, filterBy, page, activeTab]);

    useEffect(() => {
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify({sortBy, filterBy}));
    }, [sortBy, filterBy]);

    useEffect(() => {
        window.scrollTo(0, 0);
    }, [page]);

    const handleTypeChange = (value) => {
        setFilters((prev) => ({...prev, filterBy: {...prev.filterBy, type: value}}));
        setPage(1);
    };

    const handleAirStatusChange = (value) => {
        setFilters((prev) => ({...prev, filterBy: {...prev.filterBy, airStatus: value}}));
        setPage(1);
    };

    const handleSortByChange = (value) => {
        setFilters((prev) => ({...prev, sortBy: value}));
    };

    return (
        <StyledSeasonTab>
            <SelectWrapper>
                <CommonSelect
                    value={`Type: ${toTypeLabel(filterBy.type)}`}
                    onChange={handleTypeChange}
                >
                    <CommonSelect.Option value="tv">TV</CommonSelect.Option>
                </CommonSelect>

                <CommonSelect
                    value={`Air Status: ${toAirStatusLabel(filterBy.airStatus)}`}
                    onChange={handleAirStatusChange}
                >
                    <CommonSelect.Option value="all">All</CommonSelect.Option>
                    <CommonSelect.Option value="currently_airing">Airing</CommonSelect.Option>
                    <CommonSelect.Option value="finished_airing">Ended</CommonSelect.Option>
                </CommonSelect>

                <CommonSelect
                    value={`Sort: ${sortBy.charAt(0).toUpperCase() + sortBy.slice(1)}`}
                    onChange={handleSortByChange}
                >
                    <CommonSelect.Option value="score">Score</CommonSelect.Option>
                    <CommonSelect.Option value="votes">Votes</CommonSelect.Option>
                    <CommonSelect.Option value="rank">Rank</CommonSelect.Option>
                    <CommonSelect.Option value="members">Members</CommonSelect.Option>
                    <CommonSelect.Option value="popularity">Popularity</CommonSelect.Option>
                </CommonSelect>
            </SelectWrapper>

            <CustomTabs
                tabs={tabs}
                defaultActiveKey="4"
                activeKey={activeTab}
                onChange={handleTabChange}
            />
        </StyledSeasonTab>
    );
};

export default SeasonTab;
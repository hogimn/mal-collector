import {useMemo} from "react";
import {useNavigate} from "react-router-dom";
import {getCurrentSeason, getCurrentSeasonYear} from "../../utils/dateUtil";
import PageTemplate from "../../components/layout/PageTemplate";
import {ArchiveContainer, SeasonButton, YearLabel, YearRow} from "./SeasonArchive.style";

const SEASONS = ["winter", "spring", "summer", "fall"];

const SeasonArchive = () => {
    const navigate = useNavigate();

    const currentYear = getCurrentSeasonYear();
    const currentSeason = getCurrentSeason();
    const seasonIndex = SEASONS.indexOf(currentSeason.toLowerCase());

    const years = useMemo(() => {
        const list = [];
        for (let y = currentYear; y >= 2000; y--) {
            list.push(y);
        }
        return list;
    }, [currentYear]);

    return (
        <PageTemplate>
            <ArchiveContainer>
                {years.map((year) => (
                    <YearRow key={year}>
                        <YearLabel>{year}</YearLabel>
                        {SEASONS.map((season, idx) => {
                            if (year === currentYear && idx > seasonIndex) return null;

                            return (
                                <SeasonButton
                                    key={season}
                                    onClick={() =>
                                        navigate(`/season-anime?year=${year}&season=${season}`)
                                    }
                                >
                                    {season.charAt(0).toUpperCase() + season.slice(1)}
                                </SeasonButton>
                            );
                        })}
                    </YearRow>
                ))}
            </ArchiveContainer>
        </PageTemplate>
    );
};

export default SeasonArchive;
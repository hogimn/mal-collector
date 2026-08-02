import {useLocation} from "react-router-dom";
import PageTemplate from "../../components/layout/PageTemplate";
import SeasonTab from "./components/SeasonTab";

const SeasonAnime = () => {
    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const year = queryParams.get("year");
    const season = queryParams.get("season");

    return (
        <PageTemplate>
            <SeasonTab season={season} year={year}/>
        </PageTemplate>
    );
};

export default SeasonAnime;
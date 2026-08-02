import "./App.css";
import {HashRouter, Navigate, Route, Routes} from "react-router-dom";
import SeasonAnime from "./pages/SeasonAnime";
import {registerCharts} from "./config/chartConfig";
import SeasonArchive from "./pages/SeasonArchive";

registerCharts();

function App() {
    return (
        <HashRouter>
            <Routes>
                <Route
                    path={"/"}
                    element={<Navigate to={"/season-anime"} replace/>}
                />
                <Route path={"/season-anime"} element={<SeasonAnime/>}/>
                <Route path={"/season-archive"} element={<SeasonArchive/>}/>
            </Routes>
        </HashRouter>
    );
}

export default App;
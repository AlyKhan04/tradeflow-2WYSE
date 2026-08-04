import { useBreaks } from "../context/BreakContext.jsx";

export default function Navbar() {
    const { openCount } = useBreaks();

    return (
        <header className="topbar">
            <span className="logo">DB · TradeFlow</span>

            <span className="user">
                Logged in as <strong>trader</strong>

                <span className="badge">
                    {openCount}
                </span>
            </span>
        </header>
    );
}
import React from 'react';
import { Navigate, Route, Routes, NavLink } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import Trades from './pages/Trades.jsx';
import AddTradeForm from './components/AddTradeForm.jsx';
import Recon from './pages/Recon.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx';
import { BreakProvider, useBreaks } from './context/BreakContext.jsx';

export default function App() {
    return (
        <BreakProvider>
        <div className="layout">
            <header className="topbar">
                <span className="logo">DB · TradeFlow</span>
                <span className="user">Logged in as <strong>trader</strong></span>
                <OpenBreakBadge />
            </header>

            <div className="main">
                <nav className="sidebar" aria-label="Primary">
                    <ul>
                        <li><NavLink to="/dashboard" className={navClass}>Dashboard</NavLink></li>
                        <li><NavLink to="/trades"    className={navClass}>Trades</NavLink></li>
                        <li><NavLink to="/trades/new" className={navClass}>+ New Trade</NavLink></li>
                        <li><NavLink to="/recon"     className={navClass}>Recon Breaks</NavLink></li>
                    </ul>
                </nav>

                <section className="content">
                    <Routes>
                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route
                            path="/dashboard"
                            element={
                                <ErrorBoundary>
                                    <Dashboard />
                                </ErrorBoundary>
                            }
                        />
                        <Route
                            path="/trades"
                            element={
                                <ErrorBoundary>
                                    <Trades />
                                </ErrorBoundary>
                            }
                        />
                        <Route
                            path="/trades/new"
                            element={
                                <ErrorBoundary>
                                    <AddTradeForm />
                                </ErrorBoundary>
                            }
                        />
                        <Route
                            path="/recon"
                            element={
                                <ErrorBoundary>
                                    <Recon />
                                </ErrorBoundary>
                            }
                        />
                        <Route
                            path="*"
                            element={
                                <ErrorBoundary>
                                    <NotFound />
                                </ErrorBoundary>
                            }
                        />
                    </Routes>
                </section>
            </div>
        </div>
        </BreakProvider>
    );
}

function navClass({ isActive }) {
    return isActive ? 'active' : '';
}

function OpenBreakBadge() {
    const { openCount } = useBreaks();
    return <span className="badge">Open breaks: {openCount}</span>;
}

function NotFound() {
    return (
        <div>
            <h2>404 — Not Found</h2>
            <NavLink to="/dashboard">Back to dashboard</NavLink>
        </div>
    );
}
import { createContext, useContext, useReducer } from "react";

const BreakContext = createContext();

const initialState = {
    openCount: 0
};

function breakReducer(state, action) {
    switch (action.type) {

        case "HYDRATE":
            return {
                ...state,
                openCount: action.payload
            };

        case "RESOLVE":
            return {
                ...state,
                openCount: Math.max(0, state.openCount - 1)
            };

        case "REOPEN":
            return {
                ...state,
                openCount: state.openCount + 1
            };

        default:
            return state;
    }
}


export function BreakProvider({ children }) {

    const [state, dispatch] = useReducer(
        breakReducer,
        initialState
    );

    return (
        <BreakContext.Provider value={{...state, dispatch}}>
            {children}
        </BreakContext.Provider>
    );
}


export function useBreaks() {

    const context = useContext(BreakContext);

    if (!context) {
        throw new Error(
            "useBreaks must be used inside BreakProvider"
        );
    }

    return context;
}
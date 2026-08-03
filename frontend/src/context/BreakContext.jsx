import { createContext, useContext, useReducer } from 'react';

const initialState = { openCount: 0, lastUpdated: 0 };

function reducer(state, action) {
  switch (action.type) {
    case 'HYDRATE':
      return {
        ...state,
        openCount: action.payload ?? state.openCount,
        lastUpdated: Date.now(),
      };
    case 'RESOLVE':
      return {
        ...state,
        openCount: Math.max(0, state.openCount - 1),
        lastUpdated: Date.now(),
      };
    case 'REOPEN':
      return {
        ...state,
        openCount: state.openCount + 1,
        lastUpdated: Date.now(),
      };
    default:
      return state;
  }
}

const BreakContext = createContext(null);

export function BreakProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  return (
    <BreakContext.Provider value={{ ...state, dispatch }}>
      {children}
    </BreakContext.Provider>
  );
}

export function useBreaks() {
  const context = useContext(BreakContext);
  if (!context) {
    throw new Error('useBreaks must be used within a BreakProvider');
  }
  return context;
}

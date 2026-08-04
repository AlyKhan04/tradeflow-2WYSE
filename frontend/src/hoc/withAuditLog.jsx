import { forwardRef, useEffect, useRef } from 'react';

export function withAuditLog(Component, label) {
  const componentName = Component.displayName || Component.name || 'Component';
  const displayName = `withAuditLog(${componentName})`;
  const logName = label || componentName;

  const AuditLoggedComponent = forwardRef((props, ref) => {
    const mountedRef = useRef(false);

    useEffect(() => {
      console.log(`[audit] ${logName} mounted`);
      mountedRef.current = true;
    }, [logName]);

    console.log(`[audit] ${logName} render`);

    return <Component ref={ref} {...props} />;
  });

  AuditLoggedComponent.displayName = displayName;
  return AuditLoggedComponent;
}

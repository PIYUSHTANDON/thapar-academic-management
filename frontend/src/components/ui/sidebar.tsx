import React, { useState, createContext, useContext } from "react";
import { NavLink, Link } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { Menu, X } from "lucide-react";

interface SidebarLinkItem {
  label: string;
  href: string;
  icon: React.ReactNode;
  dot?: boolean;
}

interface SidebarContextProps {
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
  animate: boolean;
}

const SidebarContext = createContext<SidebarContextProps | undefined>(undefined);

export const useSidebar = () => {
  const context = useContext(SidebarContext);
  if (!context) {
    throw new Error("useSidebar must be used within a SidebarProvider");
  }
  return context;
};

export const SidebarProvider = ({
  children,
  open: openProp,
  setOpen: setOpenProp,
  animate = true,
}: {
  children: React.ReactNode;
  open?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  animate?: boolean;
}) => {
  const [openState, setOpenState] = useState(false);

  const open = openProp !== undefined ? openProp : openState;
  const setOpen = setOpenProp !== undefined ? setOpenProp : setOpenState;

  return (
    <SidebarContext.Provider value={{ open, setOpen, animate }}>
      {children}
    </SidebarContext.Provider>
  );
};

export const Sidebar = ({
  children,
  open,
  setOpen,
  animate = true,
}: {
  children: React.ReactNode;
  open?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  animate?: boolean;
}) => {
  return (
    <SidebarProvider open={open} setOpen={setOpen} animate={animate}>
      {children}
    </SidebarProvider>
  );
};

export const SidebarBody = (props: React.ComponentProps<typeof motion.aside>) => {
  return (
    <>
      <DesktopSidebar {...props} />
      <MobileSidebar {...(props as unknown as React.ComponentProps<"div">)} />
    </>
  );
};

export const DesktopSidebar = ({
  className = "",
  children,
  ...props
}: React.ComponentProps<typeof motion.aside>) => {
  const { open, setOpen, animate } = useSidebar();

  return (
    <motion.aside
      className={`sidebar collapsible-sidebar ${open ? "is-expanded" : "is-collapsed"} ${className}`}
      animate={{
        width: animate ? (open ? "240px" : "72px") : "240px",
      }}
      transition={{
        duration: 0.22,
        ease: [0.4, 0, 0.2, 1],
      }}
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      {...props}
    >
      {children}
    </motion.aside>
  );
};

export const MobileSidebar = ({
  className = "",
  children,
  ...props
}: React.ComponentProps<"div">) => {
  const { open, setOpen } = useSidebar();
  return (
    <div className={`mobile-sidebar-container ${className}`} {...props}>
      <div className="mobile-sidebar-toggle-bar">
        <button
          type="button"
          className="mobile-sidebar-btn"
          aria-label="Open navigation menu"
          onClick={() => setOpen(!open)}
        >
          <Menu size={22} />
        </button>
      </div>
      <AnimatePresence>
        {open && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="mobile-sidebar-scrim"
              onClick={() => setOpen(false)}
            />
            <motion.div
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{
                duration: 0.25,
                ease: "easeInOut",
              }}
              className="mobile-sidebar-drawer"
            >
              <button
                type="button"
                className="mobile-sidebar-close"
                aria-label="Close navigation menu"
                onClick={() => setOpen(false)}
              >
                <X size={22} />
              </button>
              {children}
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};

export const SidebarLink = ({
  link,
  className = "",
  onClick,
}: {
  link: SidebarLinkItem;
  className?: string;
  onClick?: () => void;
}) => {
  const { open, animate } = useSidebar();

  return (
    <NavLink
      to={link.href}
      className={({ isActive }) =>
        `sidebar-nav-link ${isActive ? "active" : ""} ${className}`
      }
      onClick={onClick}
      title={open ? undefined : link.label}
    >
      <span className="sidebar-nav-icon">{link.icon}</span>
      <motion.span
        animate={{
          display: animate ? (open ? "inline-block" : "none") : "inline-block",
          opacity: animate ? (open ? 1 : 0) : 1,
        }}
        transition={{ duration: 0.15 }}
        className="sidebar-nav-label"
      >
        {link.label}
      </motion.span>
      {link.dot && (
        <motion.i
          animate={{
            display: animate ? (open ? "inline-block" : "none") : "none",
            opacity: animate ? (open ? 1 : 0) : 0,
          }}
          className="nav-dot"
        />
      )}
    </NavLink>
  );
};


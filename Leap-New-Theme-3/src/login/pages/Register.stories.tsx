import type { Meta, StoryObj } from "@storybook/react";
import { createKcPageStory } from "../KcPageStory";
import { useEffect } from "react";

const { KcPageStory } = createKcPageStory({ pageId: "register.ftl" });

const meta = {
    title: "login/register.ftl",
    component: KcPageStory
} satisfies Meta<typeof KcPageStory>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
    render: () => <KcPageStory />
};

export const WithEmailAlreadyExistsError: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                messagesPerField: {
                    existsError: (fieldName: string) => {
                        return fieldName === "email";
                    },
                    get: (fieldName: string) => {
                        if (fieldName === "email") {
                            return "Email already exists";
                        }
                        return "";
                    },
                    exists: (fieldName: string) => {
                        return fieldName === "email";
                    }
                }
            }}
        />
    )
};

// Wrapper component to simulate LeapWeb URL with query parameter
const LeapWebWrapper = ({ email }: { email: string }) => {
    useEffect(() => {
        // Mock the URL for testing
        const originalSearch = window.location.search;
        Object.defineProperty(window, 'location', {
            value: {
                ...window.location,
                search: `?email=${email}`,
                href: `${window.location.origin}${window.location.pathname}?email=${email}`
            },
            writable: true
        });

        return () => {
            // Restore original location
            Object.defineProperty(window, 'location', {
                value: {
                    ...window.location,
                    search: originalSearch,
                    href: `${window.location.origin}${window.location.pathname}${originalSearch}`
                },
                writable: true
            });
        };
    }, [email]);

    return (
        <KcPageStory
            kcContext={{
                profile: {
                    attributesByName: {
                        email: { value: "" } // Empty for LeapWeb
                    }
                }
            }}
        />
    );
};

export const WithLeapWebEmailFromQueryParam: Story = {
    render: () => <LeapWebWrapper email="test@example.com" />,
    parameters: {
        docs: {
            description: {
                story: 'This story simulates the LeapWeb flow where email comes from query parameter. In real usage, the URL would be like: `/register?email=user@example.com`'
            }
        }
    }
};

export const WithLeapWebEmailFromQueryParamGmail: Story = {
    render: () => <LeapWebWrapper email="user@gmail.com" />,
    parameters: {
        docs: {
            description: {
                story: 'This story simulates the LeapWeb flow with a Gmail address that should pass validation.'
            }
        }
    }
};

export const WithGlobalEmailError: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                messagesPerField: {
                    existsError: (fieldName: string) => {
                        return fieldName === "global";
                    },
                    get: (fieldName: string) => {
                        if (fieldName === "global") {
                            return "Email already exists";
                        }
                        return "";
                    },
                    exists: (fieldName: string) => {
                        return fieldName === "global";
                    }
                }
            }}
        />
    )
};
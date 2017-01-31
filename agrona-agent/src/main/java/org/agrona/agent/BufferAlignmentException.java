package org.agrona.agent;

/**
 * Runtime Exception thrown by {@link BufferAlignmentAgent} when an unaligned memory access is detected.<br>
 * Package-protected to discourage catching since this agent should be used only for testing and debugging
 */
class BufferAlignmentException extends RuntimeException
{

    private static final long serialVersionUID = 4196043654912374628L;

    BufferAlignmentException(String message)
    {
        super(message);
    }

}

package org.agrona.agent;

class BufferAlignmentException extends RuntimeException
{

    private static final long serialVersionUID = 4196043654912374628L;

    BufferAlignmentException(String message)
    {
        super(message);
    }

}

package com.ugav.battalion.util;

import java.io.PrintStream;

public class Logger {

	private boolean enable;
	private PrintStream out;

	public Logger() {
		this(false);
	}

	public Logger(boolean enable) {
		this.enable = enable;
		out = System.out;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void setPrintStream(PrintStream printStream) {
		this.out = printStream;
	}

	public void dbgln() {
		if (enable)
			out.println();
	}

	public void dbg(byte s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(byte s) {
		if (enable)
			out.println(s);
	}

	public void dbg(char s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(char s) {
		if (enable)
			out.println(s);
	}

	public void dbg(short s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(short s) {
		if (enable)
			out.println(s);
	}

	public void dbg(int s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(int s) {
		if (enable)
			out.println(s);
	}

	public void dbg(long s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(long s) {
		if (enable)
			out.println(s);
	}

	public void dbg(float s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(float s) {
		if (enable)
			out.println(s);
	}

	public void dbg(double s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(double s) {
		if (enable)
			out.println(s);
	}

	public void dbg(boolean s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(boolean s) {
		if (enable)
			out.println(s);
	}

	public void dbg(Object s) {
		if (enable)
			out.print(s);
	}

	public void dbgln(Object s) {
		if (enable)
			out.println(s);
	}

	public void dbg(Object arg1, Object arg2) {
		if (enable)
			out.print(String.valueOf(arg1) + arg2);
	}

	public void dbg(Object arg1, Object arg2, Object arg3) {
		if (enable)
			out.print(String.valueOf(arg1) + arg2 + arg3);
	}

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4) {
		if (enable)
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4);
	}

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		if (enable)
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5);
	}

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		if (enable)
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6);
	}

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		if (enable)
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7);
	}

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
			Object arg8) {
		if (enable)
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8);
	}

	public void dbg(Object arg1, Object... args) {
		if (enable) {
			StringBuilder builder = new StringBuilder();
			builder.append(arg1);
			for (Object arg : args)
				builder.append(arg);
			out.print(builder.toString());
		}
	}

	public void dbgln(Object arg1, Object arg2) {
		if (enable)
			out.println(String.valueOf(arg1) + arg2);
	}

	public void dbgln(Object arg1, Object arg2, Object arg3) {
		if (enable)
			out.println(String.valueOf(arg1) + arg2 + arg3);
	}

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4) {
		if (enable)
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4);
	}

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		if (enable)
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5);
	}

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		if (enable)
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6);
	}

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		if (enable)
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7);
	}

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
			Object arg8) {
		if (enable)
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8);
	}

	public void dbgln(Object arg1, Object... args) {
		if (enable) {
			StringBuilder builder = new StringBuilder();
			builder.append(arg1);
			for (Object arg : args)
				builder.append(arg);
			out.println(builder.toString());
		}
	}

	public void dbgf(String s, Object arg1) {
		if (enable)
			out.format(s, arg1);
	}

	public void dbgf(String s, Object arg1, Object arg2) {
		if (enable)
			out.format(s, arg1, arg2);
	}

	public void dbgf(String s, Object arg1, Object arg2, Object arg3) {
		if (enable)
			out.format(s, arg1, arg2, arg3);
	}

	public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4) {
		if (enable)
			out.format(s, arg1, arg2, arg3, arg4);
	}

	public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		if (enable)
			out.format(s, arg1, arg2, arg3, arg4, arg5);
	}

	public void dbgf(String s, Object... args) {
		if (enable)
			out.format(s, args);
	}

	public void printExec(Runnable exec) {
		if (enable)
			exec.run();
	}

}
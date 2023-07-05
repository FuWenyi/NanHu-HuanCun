module TS5N28HPCPLVTA1024X16M2F(
    Q, CLK, CEB, WEB, A, D
);
parameter Bits = 64;
parameter Word_Depth = 1024;
parameter Add_Width = 10;

output  reg [Bits-1:0]      Q;
input                   CLK;
input                   CEB;
input                   WEB;
input   [Add_Width-1:0] A;
input   [Bits-1:0]      D;

reg [Bits-1:0] ram [0:Word_Depth-1];
always @(posedge CLK) begin
    if(!CEB && !WEB) begin
        ram[A] <= D;
    end
    Q <= !CEB && WEB ? ram[A] : {4{$random}};
end

endmodule

module CKLNQD12BWP40P140LVT (
  input  wire TE,
  input  wire E,
  input  wire CP,
  output wire Q
);

  wire clk_en;
  reg  clk_en_reg;

  assign clk_en = E | TE;

  always @(posedge CP) 
    begin
      clk_en_reg = clk_en;
    end

  assign Q = CP & clk_en_reg;

endmodule // Copy from Xihu
